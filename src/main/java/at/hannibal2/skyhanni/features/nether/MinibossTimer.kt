package at.hannibal2.skyhanni.features.nether

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.nether.MinibossTimer.MiniBoss.Companion.isSpawned
import at.hannibal2.skyhanni.features.nether.MinibossTimer.MiniBoss.Companion.isTimerKnown
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils.isInside
import at.hannibal2.skyhanni.utils.LocationUtils.isPlayerInside
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.tileentity.TileEntityBeacon
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object MinibossTimer {

    private val patternGroup = RepoPattern.group("crimson.miniboss")

    /**
     * REGEX-TEST: §c§lBEWARE - Bladesoul Is Spawning.
     */
    private val spawnPattern by patternGroup.pattern(
        "spawn",
        "§c§lBEWARE - (?<name>.+) Is Spawning\\."
    )

    /**
     * REGEX-TEST: §f                            §r§6§lBLADESOUL DOWN!
     */
    private val downPattern by patternGroup.pattern(
        "down",
        "§f\\s*§r§6§l(?<name>.+) DOWN!"
    )

    private var currentAreaBoss: MiniBoss? = null

    var display: Renderable? = null

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!IslandType.CRIMSON_ISLE.isInIsland()) return
        val message = event.message
        downPattern.matchMatcher(message) {
            val miniBoss = MiniBoss.fromName(group("name")) ?: run {
                println("didnt detect from ${group("name")}")
                return
            }
            miniBoss.timer = SimpleTimeMark.now() + 2.minutes
            miniBoss.spawned = false
            miniBoss.possibleTimer = null
            miniBoss.foundBeacon = null
            update()
            return
        }
        spawnPattern.matchMatcher(message) {
            val miniBoss = MiniBoss.fromName(group("name")) ?: return
            miniBoss.spawned = true
            miniBoss.possibleTimer = null
            miniBoss.foundBeacon = null
            update()
            return
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!IslandType.CRIMSON_ISLE.isInIsland()) return
        val renderable = display ?: drawDisplay()
        Position(10, 10).renderRenderables(listOf(renderable), posLabel = "Miniboss Timer")
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!IslandType.CRIMSON_ISLE.isInIsland()) return
        updateArea()
        update()
    }

    private fun updateArea() {
        MiniBoss.entries.forEach {
            if (it.lastSeenArea.passedSince() > 2.minutes) {
                it.timer = null
                it.possibleTimer = null
                it.foundBeacon = null
                it.spawned = null
            }
        }
        currentAreaBoss = MiniBoss.entries.firstOrNull {
            it.area.isPlayerInside()
        }
        currentAreaBoss?.lastSeenArea = SimpleTimeMark.now()
        val boss = currentAreaBoss ?: return)
        if (boss.isTimerKnown()) return

        val isBossInArea = MobData.skyblockMobs.filter {
            it.name == boss.displayName
        }.any { boss.area.isInside(it.baseEntity.position.toLorenzVec()) }
        if (isBossInArea) {
            boss.spawned = true
            boss.foundBeacon = null
            boss.possibleTimer = null
            return
        } else boss.spawned = false

        val isThereBeacon = EntityUtils.getAllTileEntities().filter { it is TileEntityBeacon }.any {
            boss.area.isInside(it.pos.toLorenzVec())
        }
        if (boss.foundBeacon == true && !isThereBeacon) {
            boss.foundBeacon = false
            boss.possibleTimer = null
            boss.timer = SimpleTimeMark.now() + 1.minutes
            return
        }
        if (boss.possibleTimer != null) return
        if (isThereBeacon && boss.foundBeacon == null) {
            boss.foundBeacon = true
            boss.possibleTimer = SimpleTimeMark.now() + 1.minutes to SimpleTimeMark.now() + 2.minutes
            return
        }
        if (!isThereBeacon && boss.foundBeacon == null) {
            boss.foundBeacon = false
            boss.possibleTimer = SimpleTimeMark.now() to SimpleTimeMark.now() + 1.minutes
        }
    }

    private fun update() {
        display = drawDisplay()
    }

    fun drawDisplay(): Renderable {
        val lines = MiniBoss.entries.map {
            val possibleTimer = it.possibleTimer
            Renderable.string(buildString {
                append("§b${it.displayName}: ")
                if (it.isSpawned()) append("§aSPAWNED!")
                else if (it.isTimerKnown()) append("§e${it.timer?.timeUntil()?.format()}")
                else if (possibleTimer != null) {
                    val (start, end) = possibleTimer
                    if (start.timeUntil().isNegative()) append("§e~Now - ")
                    else append("§e~${start.timeUntil().format()} - ")
                    if (end.timeUntil().isNegative()) append("§cNow")
                    else append("§c${end.timeUntil().format()}")
                } else append("§cUnknown")
            })
        }
        return Renderable.verticalContainer(lines)
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        MiniBoss.entries.forEach {
            it.timer = null
            it.possibleTimer = null
            it.foundBeacon = null
            it.spawned = null
            it.lastSeenArea = SimpleTimeMark.farPast()
        }
        currentAreaBoss = null
    }

    @SubscribeEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Crimson Isle Miniboss")
        event.addIrrelevant {
            add("Current Area Boss: ${currentAreaBoss?.displayName}")
            MiniBoss.entries.forEach {
                add(it.displayName)
                add("Timer ${it.timer?.timeUntil()?.format()}")
                add(
                    "Possible Timer ${it.possibleTimer?.first?.timeUntil()?.format()} - " +
                        "${it.possibleTimer?.second?.timeUntil()?.format()}"
                )
                add("Found Beacon ${it.foundBeacon}")
                add("Spawned ${it.spawned}")
                add("Last Seen Area ${it.lastSeenArea.passedSince().format()}")
            }
        }
    }

    enum class MiniBoss(
        val displayName: String,
        val area: AxisAlignedBB,
        var timer: SimpleTimeMark? = null,
        var possibleTimer: Pair<SimpleTimeMark, SimpleTimeMark>? = null,
        var foundBeacon: Boolean? = null,
        var spawned: Boolean? = null,
        var lastSeenArea: SimpleTimeMark = SimpleTimeMark.farPast()
    ) {
        BLADESOUL(
            "Bladesoul",
            LorenzVec(-330, 80, -486).axisAlignedTo(LorenzVec(-257, 107, -545))
        ),
        MAGE_OUTLAW(
            "Mage Outlaw",
            LorenzVec(-200, 98, -843).axisAlignedTo(LorenzVec(-162, 116, -878))
        ),
        BARBARIAN_DUKE_X(
            "Barbarian Duke X",
            LorenzVec(-550, 101, -890).axisAlignedTo(LorenzVec(-522, 131, -918))
        ),
        ASHFANG(
            "Ashfang",
            LorenzVec(-453, 155, -1050).axisAlignedTo(LorenzVec(-523, 131, -980))
        ),
        MAGMA_BOSS(
            "Magma Boss",
            LorenzVec(-318, 59, -751).axisAlignedTo(LorenzVec(-442, 90, -851))
        ),
        ;

        override fun toString() = displayName

        companion object {
            fun fromName(spawnName: String) = entries.firstOrNull {
                it.displayName.removeColor().lowercase() == spawnName.lowercase()
            }

            fun MiniBoss.isTimerKnown(): Boolean {
                val timer = timer ?: return false
                return timer.passedSince() < 2.minutes + 5.seconds
            }

            fun MiniBoss.isSpawned(): Boolean {
                if (spawned == true) return true
                val timer = timer ?: return false
                return (timer.passedSince() - 2.minutes) in 0.seconds..20.seconds
            }
        }
    }

}
