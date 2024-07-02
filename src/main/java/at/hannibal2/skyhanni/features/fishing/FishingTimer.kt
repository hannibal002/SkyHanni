package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzKeyPressEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.SeaCreatureFishEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyClicked
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RecalculatingValue
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.TimeLimitedSet
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils.format
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FishingTimer {

    private val config get() = SkyHanniMod.feature.fishing.barnTimer
    private val barnLocation = LorenzVec(108, 89, -252)
    private val mobMap = mutableMapOf<Mob, SimpleTimeMark>()

    private val lastSeaCreatureFished = SimpleTimeMark.farPast()
    private var lastNameFished: String? = null
    private val recentMobs = TimeLimitedSet<Mob>(2.seconds)
    private val currentCap = RecalculatingValue(1.seconds) {
        when (LorenzUtils.skyBlockIsland) {
            IslandType.CRYSTAL_HOLLOWS -> 20
            IslandType.CRIMSON_ISLE -> 5
            else -> config.fishingCapAmount
        }
    }

    private var rightLocation = false
    private var currentCount = 0
    private var startTime = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        updateLocation()
        if (startTime.passedSince().inWholeSeconds - config.alertTime in 0..3) {
            playSound()
        }
        if (config.wormLimitAlert && IslandType.CRYSTAL_HOLLOWS.isInIsland()) {
            if (currentCount >= 20) {
                playSound()
                LorenzUtils.sendTitle("§cWORM CAP FULL!!!", 2.seconds)
            }
        } else if (config.fishingCapAlert && currentCount >= currentCap.getValue()) {
            playSound()
        }
    }

    private fun playSound() = SoundUtils.repeatSound(250, 4, SoundUtils.plingSound)

    @SubscribeEvent
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        if (!isEnabled()) return
        if (event.mob.name !in SeaCreatureManager.allFishingMobs) return
        recentMobs.add(event.mob)
        handle()
    }

    @SubscribeEvent
    fun onMobDeSpawn(event: MobEvent.DeSpawn.SkyblockMob) {
        if (!isEnabled()) return
        mobMap -= event.mob
        recentMobs.remove(event.mob)
        updateInfo()
    }

    @SubscribeEvent
    fun onSeaCreatureFish(event: SeaCreatureFishEvent) {
        if (!isEnabled()) return
        if (!rightLocation) return
        lastNameFished = event.seaCreature.name
        handle()
    }

    private fun handle() {
        if (lastSeaCreatureFished.passedSince() > 2.seconds) return
        val mob = recentMobs.toSet().filter { it.name == lastNameFished }
            .minByOrNull { it.baseEntity.distanceToPlayer() } ?: return

        mobMap[mob] = SimpleTimeMark.now()
        recentMobs.clear()
        lastNameFished = null
    }

    @SubscribeEvent
    fun onKeyPress(event: LorenzKeyPressEvent) {
        if (!isEnabled()) return
        if (Minecraft.getMinecraft().currentScreen != null) return
        if (config.manualResetTimer.isKeyClicked()) {
            mobMap.replaceAll { _, _ ->
                SimpleTimeMark.now()
            }
        }
    }

    private fun updateInfo() {
        currentCount = mobMap.entries.sumOf {
            1 + it.key.extraEntities.size
        }
        startTime = mobMap.maxByOrNull { it.value.passedSince() }?.value ?: SimpleTimeMark.farPast()
    }

    private fun updateLocation() {
        rightLocation = when (LorenzUtils.skyBlockIsland) {
            IslandType.CRYSTAL_HOLLOWS -> config.crystalHollows.get()
            IslandType.CRIMSON_ISLE -> config.crimsonIsle.get()
            IslandType.WINTER -> config.winterIsland.get()
            IslandType.HUB -> barnLocation.distanceToPlayer() < 50
            IslandType.PRIVATE_ISLAND -> config.forStranded.get() && LorenzUtils.isStrandedProfile
            else -> false
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (!rightLocation) return
        if (currentCount == 0) return
        if (!FishingAPI.isFishing()) return

        val text = createDisplay()
        config.pos.renderString(text, posLabel = "BarnTimer")
    }

    private fun createDisplay(): String {
        val passedSince = startTime.passedSince()
        val timeColor = if (passedSince > config.alertTime.seconds) "§c" else "§e"
        val timeFormat = passedSince.format(TimeUnit.MINUTE)
        val countColor = if (config.fishingCapAlert && currentCount >= currentCap.getValue()) "§c" else "§e"
        val name = StringUtils.pluralize(currentCount, "sea creature")
        return "$timeColor$timeFormat §8($countColor$currentCount §b$name§8)"
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled.get()

    @SubscribeEvent
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
