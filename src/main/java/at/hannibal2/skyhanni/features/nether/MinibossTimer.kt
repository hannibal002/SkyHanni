package at.hannibal2.skyhanni.features.nether

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.mob.MobData
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
            val miniBoss = MiniBoss.fromName(group("name")) ?: return
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
        currentAreaBoss = MiniBoss.entries.firstOrNull {
            it.area.isPlayerInside()
        }
        currentAreaBoss?.lastSeenArea = SimpleTimeMark.now()
        val boss = currentAreaBoss ?: return
        if (boss.isTimerKnown()) return
        if (boss.spawned == true) return

        val isBossInArea = MobData.skyblockMobs.filter {
            it.name == boss.displayName
        }.any { boss.area.isInside(it.baseEntity.position.toLorenzVec()) }
        if (isBossInArea) {
            boss.spawned = true
            boss.foundBeacon = null
            boss.possibleTimer = null
            return
        }
        boss.spawned = false

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
                append(it.displayName)
                if (it.isSpawned()) append(" §aSPAWNED!")
                else if (it.isTimerKnown()) append(it.timer?.timeUntil()?.format())
                else if (possibleTimer != null) {
                    val (start, end) = possibleTimer
                    if (start.timeUntil().isNegative()) append(" §e~Now - ")
                    else append(" §e~${start.timeUntil().format()} - ")
                    if (end.timeUntil().isNegative()) append("§cNow")
                    else append("§c${end.timeUntil().format()}")
                } else append(" §7Unknown")
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
            LorenzVec(0, 0, 0).axisAlignedTo(LorenzVec(0, 0, 0))
        ),
        MAGE_OUTLAW(
            "Mage Outlaw",
            LorenzVec(0, 0, 0).axisAlignedTo(LorenzVec(0, 0, 0))
        ),
        BARBARIAN_DUKE_X(
            "Barbarian Duke X",
            LorenzVec(0, 0, 0).axisAlignedTo(LorenzVec(0, 0, 0))
        ),
        ASHFANG(
            "Ashfang",
            LorenzVec(0, 0, 0).axisAlignedTo(LorenzVec(0, 0, 0))
        ),
        MAGMA_BOSS(
            "Magma Boss",
            LorenzVec(0, 0, 0).axisAlignedTo(LorenzVec(0, 0, 0))
        ),
        ;

        override fun toString() = displayName

        companion object {
            fun fromName(spawnName: String) = entries.firstOrNull {
                it.displayName.removeColor().lowercase() == spawnName.lowercase()
            }

            fun MiniBoss.isTimerKnown(): Boolean {
                val timer = timer ?: return false
                return timer.passedSince() < 2.minutes + 20.seconds
            }

            fun MiniBoss.isSpawned(): Boolean {
                if (spawned == true) return true
                val timer = timer ?: return false
                return timer.passedSince() < 2.minutes + 20.seconds
            }
        }
    }

}
