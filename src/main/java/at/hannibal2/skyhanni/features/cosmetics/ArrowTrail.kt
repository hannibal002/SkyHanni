package at.hannibal2.skyhanni.features.cosmetics

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.getPrevLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.entity.projectile.EntityArrow
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.toChromaColor
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ArrowTrail {

    private val config = SkyHanniMod.feature.misc.cosmeticConfig.arrowTrailConfig

    private data class Line(val start: LorenzVec, val end: LorenzVec, val startTick: Long)

    private val listAllArrow = mutableListOf<Line>()
    private val listYourArrow = mutableListOf<Line>()

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.enabled) return
        listAllArrow.removeIf { it.startTick > getCurrentTick() + config.secondsAlive * 20 }
        listYourArrow.removeIf { it.startTick > getCurrentTick() + config.secondsAlive * 20 }
        EntityUtils.getEntities<EntityArrow>().forEach {
            if (it.shootingEntity == Minecraft.getMinecraft().thePlayer) {
                listYourArrow.add(Line(it.getPrevLorenzVec(), it.getLorenzVec(), getCurrentTick()))
            } else {
                listAllArrow.add(Line(it.getPrevLorenzVec(), it.getLorenzVec(), getCurrentTick()))
            }
        }
    }

    private fun getCurrentTick() = Minecraft.getMinecraft().theWorld.worldTime //TODO move it to the "correct" place

    @SubscribeEvent
    fun onWorldRender(event: LorenzRenderWorldEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.enabled) return
        val playerArrowColor = if(config.handlePlayerArrowsDifferently) config.playerArrowColor.toChromaColor() else
            config.arrowColor.toChromaColor()
        val arrowColor = config.arrowColor.toChromaColor()
        listYourArrow.forEach {
            event.draw3DLine(it.start,it.end,playerArrowColor, config.lineWidth,true)
        }
        if(!config.hideOtherArrows){
            listAllArrow.forEach {
                event.draw3DLine(it.start,it.end,arrowColor, config.lineWidth,true)
            }
        }
    }
}