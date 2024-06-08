package at.hannibal2.skyhanni.features.nether

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
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
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

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

    private val currentArea: MiniBoss? = null

    var display: Renderable? = null

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!IslandType.CRIMSON_ISLE.isInIsland()) return
        val message = event.message
        downPattern.matchMatcher(message) {
            val miniBoss = MiniBoss.fromName(group("name")) ?: return
            miniBoss.timer = SimpleTimeMark.now() + 2.minutes
            update()
            return
        }
        spawnPattern.matchMatcher(message) {
            val miniBoss = MiniBoss.fromName(group("name")) ?: return
            miniBoss.timer = SimpleTimeMark.now()
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
        updateOnArea()
        update()
    }

    private fun updateOnArea() {
        MiniBoss.entries.firstOrNull {
            it.area.isPlayerInside()
        }
    }

    private fun update() {
        display = drawDisplay()
    }

    fun drawDisplay(): Renderable {
        val lines = MiniBoss.entries.sortedBy {
            it.timer?.timeUntil() ?: Duration.INFINITE
        }.map {
            val timer = it.timer

            val text = when {
                timer == null -> "§cUnknown"
                timer.isInPast() -> "§aREADY"
                else -> "§e${timer.timeUntil().format()}"
            }
            Renderable.string("${it.displayName} §r${text}")
        }
        return Renderable.verticalContainer(lines)
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        MiniBoss.entries.forEach { it.timer = null }
    }

    enum class MiniBoss(
        val displayName: String,
        val area: AxisAlignedBB,
        var timer: SimpleTimeMark? = null,
        var lastSeen: SimpleTimeMark = SimpleTimeMark.farPast()
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
        }
    }

}
