package at.hannibal2.skyhanni.features.rift

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

class HighlightBlobbercysts {

    private val config get() = SkyHanniMod.feature.rift
    private val entityList = mutableListOf<EntityOtherPlayerMP>()
    private val blobberName = "Blobbercyst "

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!event.isMod(5)) return
        Minecraft.getMinecraft().theWorld.loadedEntityList.filterIsInstance<EntityOtherPlayerMP>().forEach {
            if (it.name == blobberName) {
                RenderLivingEntityHelper.setEntityColor(it, Color.RED.withAlpha(80)) { true }
                RenderLivingEntityHelper.setNoHurtTime(it) { true }
                entityList.add(it)
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        if (!isEnabled()) return
        if (!config.highlightBlobbercysts) return
        entityList.clear()
    }

    @SubscribeEvent
    fun onLivingDeath(event: LivingDeathEvent) {
        if (!isEnabled()) return
        if (!config.highlightBlobbercysts) return
        if (entityList.contains(event.entity)) {
            entityList.remove(event.entity)
        }
    }

    @SubscribeEvent
    fun pre(event: RenderLivingEvent.Pre<EntityOtherPlayerMP>) {
        if (!isEnabled()) return
        if (!config.highlightBlobbercysts) return
        if (event.entity.name != blobberName) return
        GlStateManager.disableDepth()
    }

    @SubscribeEvent
    fun pre(event: RenderLivingEvent.Post<EntityOtherPlayerMP>) {
        if (!isEnabled()) return
        if (!config.highlightBlobbercysts) return
        if (event.entity.name != blobberName) return
        GlStateManager.enableDepth()
    }

    fun isEnabled() = RiftAPI.inRift() && LorenzUtils.skyBlockArea == "Colosseum"
}