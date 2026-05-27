package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.IslandJoinEvent
import at.hannibal2.skyhanni.events.entity.EntityMoveEvent
import at.hannibal2.skyhanni.events.fishing.SeaCreatureEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.ServerTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.compat.withColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.ChatFormatting
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.Component
import kotlin.reflect.KMutableProperty0
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object BarnFishingTimer {

    private val config get() = SkyHanniMod.feature.fishing.barnTimer
    private const val GLOBAL_CAP = 60
    private val warningDelay = 5.seconds
    private val hubBarnFishingLocation = LorenzVec(108, 89, -252)

    private enum class FishingCap(val island: IslandType, islandPersonalCap: Int? = null) {
        CRIMSON_ISLE(IslandType.CRIMSON_ISLE, 5),
        CRYSTAL_HOLLOWS(IslandType.CRYSTAL_HOLLOWS, 20),
        OTHERS(IslandType.NONE),
        ;

        val currentPersonalCap: Int = islandPersonalCap ?: GLOBAL_CAP
        val hasPersonalCap: Boolean = islandPersonalCap != null

        companion object {
            fun getForIsland(island: IslandType): FishingCap = entries.find { it.island == island } ?: OTHERS
        }
    }

    private enum class AlertReason(val title: String, val message: String) {
        TIME(
            "Fishing Time Limit!",
            "Reached barn fishing time limit!",
        ),
        PERSONAL_CAP(
            "Reached Personal Cap!",
            "Reached personal sea creature cap!",
        ),
        GLOBAL_CAP(
            "Reached Global Cap!",
            "Reached global sea creature cap!",
        ),
        NO_ALERT(
            "You shouldn't see this, report this as a bug",
            "You shouldn't see this, report this as a bug",
        ),
        ;

        inline val isAlert: Boolean get() = this != NO_ALERT
    }

    private var ownMobs: Int = 0
    private var otherMobs: Int = 0
    private val totalMobs: Int get() = ownMobs + otherMobs

    private var currentCap = FishingCap.OTHERS
    private var enabledInIsland = false

    private var oldestSeaCreature: LivingSeaCreatureData? = null
    private var oldestTime: ServerTimeMark = ServerTimeMark.farPast()

    private var display: Renderable? = null
    private var lastWarning: ServerTimeMark = ServerTimeMark.farPast()

    @HandleEvent
    fun onSeaCreatureSpawn(event: SeaCreatureEvent.Spawn) = event.seaCreature.handleSpawn()

    @HandleEvent
    fun onSeaCreatureReDetect(event: SeaCreatureEvent.ReDetect) = event.seaCreature.handleSpawn()

    @HandleEvent
    fun onSeaCreatureDeSpawn(event: SeaCreatureEvent.DeSpawn) = event.seaCreature.handleDeSpawn()

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed() = update()

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiRenderOverlay() {
        if (!isEnabled()) return
        display?.let {
            config.pos.renderRenderable(it, posLabel = "Fishing Timer")
        }
    }

    private fun KMutableProperty0<Int>.decrease() = set((get() - 1).coerceAtLeast(0))

    private fun LivingSeaCreatureData.handleSpawn() {
        if (isOwn) ++ownMobs else ++otherMobs
        val oldest = oldestSeaCreature
        if (oldest == null || spawnTime < oldest.spawnTime) {
            oldestSeaCreature = this
            oldestTime = spawnTime
        }
        update()
    }

    private fun LivingSeaCreatureData.handleDeSpawn() {
        val property = if (isOwn) ::ownMobs else ::otherMobs
        property.decrease()
        if (this == oldestSeaCreature) calculateOldest()
        update()
    }

    private fun update() {
        if (totalMobs == 0) {
            if (display != null) reset()
            return
        }
        if (!isEnabled()) return
        val timeSince = oldestTime.passedSince()

        val reason = shouldWarn(timeSince)
        if (reason.isAlert) {
            lastWarning = ServerTimeMark.now()
            SoundUtils.plingSound.playSound()
            TitleManager.sendTitle("§c${reason.title}", duration = 2.seconds)
            ChatUtils.chat(
                Component.literal(reason.message).withColor(ChatFormatting.RED),
                replaceSameMessage = true,
            )
        }

        val timeColor = if (reason == AlertReason.TIME) "§c" else "§a"
        val personalCapColor = if (reason == AlertReason.PERSONAL_CAP) "§c" else "§a"
        val globalCapColor = if (reason == AlertReason.GLOBAL_CAP) "§c" else "§a"

        val formatTime = timeSince.format(showMilliSeconds = false)

        display = Renderable.text(
            buildString {
                append("$timeColor$formatTime §8(")
                if (currentCap.hasPersonalCap) append("$personalCapColor$ownMobs§7/")
                append("$globalCapColor$totalMobs §bsea creatures§8)")
            },
        )

    }

    private fun shouldWarn(timeSince: Duration): AlertReason {
        with(config) {
            return when {
                lastWarning.passedSince() < warningDelay -> AlertReason.NO_ALERT
                timeAlert && timeSince >= alertTime.seconds -> AlertReason.TIME
                warnPersonalCap && currentCap.hasPersonalCap && ownMobs >= currentCap.currentPersonalCap -> AlertReason.PERSONAL_CAP
                warnGlobalCap && totalMobs >= GLOBAL_CAP -> AlertReason.GLOBAL_CAP
                else -> AlertReason.NO_ALERT
            }
        }
    }

    private fun calculateOldest() {
        oldestSeaCreature = SeaCreatureDetectionApi.getSeaCreatures().minByOrNull { it.spawnTime }
        oldestTime = oldestSeaCreature?.spawnTime ?: ServerTimeMark.farPast()
    }

    private fun reset() {
        ownMobs = 0
        otherMobs = 0
        oldestSeaCreature = null
        oldestTime = ServerTimeMark.farPast()
        display = null
        lastWarning = ServerTimeMark.farPast()
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Fishing Timer")
        event.addIrrelevant {
            add("ownMobs $ownMobs")
            add("otherMobs $otherMobs")
            add("oldestSeaCreature $oldestSeaCreature")
            add("oldestTime $oldestTime")
            add("Time Passed since Last Warning ${lastWarning.passedSince()}")
            add("display $display")

        }
    }

    @HandleEvent
    fun onIslandJoin(event: IslandJoinEvent) {
        currentCap = FishingCap.getForIsland(event.island)
        enabledInIsland = updateLocation(event.island)
    }

    @HandleEvent
    fun onIslandLeave() {
        reset()
    }

    // `event` parameter is required because of the generic
    @Suppress("UNUSED_PARAMETER")
    @HandleEvent(onlyOnIsland = IslandType.HUB)
    fun onPlayerMove(event: EntityMoveEvent<LocalPlayer>) {
        enabledInIsland = if (config.showAnywhere) true else hubBarnFishingLocation.distanceToPlayer() < 50
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled.get() && enabledInIsland

    private fun updateLocation(island: IslandType): Boolean {
        if (config.showAnywhere) return true

        return when (island) {
            IslandType.CRYSTAL_HOLLOWS -> config.crystalHollows.get()
            IslandType.CRIMSON_ISLE -> config.crimsonIsle.get()
            IslandType.WINTER -> config.winterIsland.get()
            IslandType.PRIVATE_ISLAND -> config.forStranded.get() && SkyBlockUtils.isStrandedProfile
            else -> false
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "fishing.barnTimer", "fishing.barnTimer.enabled")
        event.move(3, "fishing.barnTimerAlertTime", "fishing.barnTimer.alertTime")
        event.move(3, "fishing.barnTimerCrystalHollows", "fishing.barnTimer.crystalHollows")
        event.move(3, "fishing.barnTimerForStranded", "fishing.barnTimer.forStranded")
        event.move(3, "fishing.wormLimitAlert", "fishing.barnTimer.wormLimitAlert")
        event.move(3, "fishing.manualResetTimer", "fishing.barnTimer.manualResetTimer")
        event.move(3, "fishing.barnTimerPos", "fishing.barnTimer.pos")
    }
}
