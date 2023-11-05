package at.hannibal2.skyhanni.features.cosmetics

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.toChromaColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.getPrevLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.entity.projectile.EntityArrow
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.LinkedList
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ArrowTrail {

    private val config get() = SkyHanniMod.feature.misc.cosmeticConfig.arrowTrailConfig

    private data class Line(val start: LorenzVec, val end: LorenzVec, val deathTime: SimpleTimeMark)

    private val listAllArrow: MutableList<Line> = LinkedList<Line>()
    private val listYourArrow: MutableList<Line> = LinkedList<Line>()

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.enabled) return
        val secondsAlive = config.secondsAlive.toDouble().toDuration(DurationUnit.SECONDS)
        val time = SimpleTimeMark.now()
        val deathTime = time.plus(secondsAlive)
        if (event.isMod(2)) {
            listAllArrow.removeIf { it.deathTime.isInPast() }
            listYourArrow.removeIf { it.deathTime.isInPast() }
        }
        EntityUtils.getEntities<EntityArrow>().forEach {
            val line = Line(it.getPrevLorenzVec(), it.getLorenzVec(), deathTime)
            if (it.shootingEntity == Minecraft.getMinecraft().thePlayer) {
                listYourArrow.add(line)
            } else {
                listAllArrow.add(line)
            }
        }
    }

    @SubscribeEvent
    fun onWorldRender(event: LorenzRenderWorldEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.enabled) return
        val color = if (config.handlePlayerArrowsDifferently) config.playerArrowColor else config.arrowColor
        val playerArrowColor = color.toChromaColor()
        listYourArrow.forEach {
            event.draw3DLine(it.start, it.end, playerArrowColor, config.lineWidth, true)
        }
        if (!config.hideOtherArrows) {
            val arrowColor = config.arrowColor.toChromaColor()
            listAllArrow.forEach {
                event.draw3DLine(it.start, it.end, arrowColor, config.lineWidth, true)
            }
        }
    }

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        listAllArrow.clear()
        listYourArrow.clear()
    }
}
