package at.hannibal2.hanni.features.cosmetics

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.enums.OutsideSBFeature
import at.hannibal2.hanni.events.IslandChangeEvent
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ColorUtils.toColor
import at.hannibal2.hanni.utils.EntityUtils
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.compat.MinecraftCompat.isLocalPlayer
import at.hannibal2.hanni.utils.getLorenzVec
import at.hannibal2.hanni.utils.getPrevLorenzVec
import at.hannibal2.hanni.utils.render.WorldRenderUtils.draw3DLine
import net.minecraft.entity.projectile.EntityArrow
import java.util.LinkedList
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@HanniModule
object ArrowTrail {

    private val config get() = HanniMod.feature.gui.cosmetic.arrowTrail

    private data class Line(val start: LorenzVec, val end: LorenzVec, val deathTime: SimpleTimeMark)

    private val listAllArrow: MutableList<Line> = LinkedList<Line>()
    private val listYourArrow: MutableList<Line> = LinkedList<Line>()

    @HandleEvent
    fun onTick() {
        if (!isEnabled()) return
        val secondsAlive = config.secondsAlive.toDouble().toDuration(DurationUnit.SECONDS)
        val time = SimpleTimeMark.now()
        val deathTime = time.plus(secondsAlive)

        listAllArrow.removeIf { it.deathTime.isInPast() }
        listYourArrow.removeIf { it.deathTime.isInPast() }

        for (arrow in EntityUtils.getEntities<EntityArrow>()) {
            val line = Line(arrow.getPrevLorenzVec(), arrow.getLorenzVec(), deathTime)
            if (arrow.shootingEntity.isLocalPlayer) {
                listYourArrow.add(line)
            } else {
                listAllArrow.add(line)
            }
        }
    }

    @HandleEvent
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (!isEnabled()) return
        val color = if (config.handlePlayerArrowsDifferently) config.playerArrowColor else config.arrowColor
        listYourArrow.forEach {
            event.draw3DLine(it.start, it.end, color.toColor(), config.lineWidth, true)
        }
        if (!config.hideOtherArrows) {
            val arrowColor = config.arrowColor
            listAllArrow.forEach {
                event.draw3DLine(it.start, it.end, arrowColor.toColor(), config.lineWidth, true)
            }
        }
    }

    private fun isEnabled() = config.enabled && (SkyBlockUtils.inSkyBlock || OutsideSBFeature.ARROW_TRAIL.isSelected())

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        listAllArrow.clear()
        listYourArrow.clear()
    }
}
