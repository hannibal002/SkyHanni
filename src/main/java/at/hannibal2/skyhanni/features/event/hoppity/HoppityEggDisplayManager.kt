package at.hannibal2.skyhanni.features.event.hoppity

import at.hannibal2.skyhanni.data.mob.MobFilter.isRealPlayer
import at.hannibal2.skyhanni.events.SkyHanniRenderEntityEvent
import at.hannibal2.skyhanni.events.render.EntityRenderLayersEvent
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11

object HoppityEggDisplayManager {

    private val config get() = HoppityEggsManager.config
    private var shouldHidePlayer: Boolean = false

    private fun canChangeOpacity(entity: EntityLivingBase): Boolean {
        if (!HoppityEggLocator.isEnabled()) return false
        if (entity !is EntityPlayer) return false
        if (entity == LorenzUtils.getPlayer()) return false
        if (!entity.isRealPlayer()) return false
        return config.playerOpacity < 100
    }

    @SubscribeEvent
    fun onPreRenderPlayer(event: SkyHanniRenderEntityEvent.Pre<EntityLivingBase>) {
        if (!canChangeOpacity(event.entity)) return

        shouldHidePlayer = HoppityEggLocator.sharedEggLocation?.let { event.entity.distanceTo(it) < 4.0 }
            ?: HoppityEggLocator.possibleEggLocations.any { event.entity.distanceTo(it) < 4.0 }

        if (!shouldHidePlayer) return
        if (config.playerOpacity <= 0) return event.cancel()

        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.color(1.0f, 1.0f, 1.0f, config.playerOpacity / 100f)
    }

    @SubscribeEvent
    fun onPostRenderPlayer(event: SkyHanniRenderEntityEvent.Post<EntityLivingBase>) {
        if (!canChangeOpacity(event.entity)) return

        GlStateManager.color(1f, 1f, 1f, 1f)
        GlStateManager.disableBlend()
    }

    @SubscribeEvent
    fun onRenderPlayerLayers(event: EntityRenderLayersEvent.Pre<EntityLivingBase>) {
        if (!canChangeOpacity(event.entity)) return
        if (!shouldHidePlayer) return
        event.cancel()
    }
}
