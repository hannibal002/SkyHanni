package at.hannibal2.skyhanni.features.rift.area.colosseum

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.features.rift.everywhere.RiftAPI
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.canSee
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

class BlobbercystsHighlight {

    private val config get() = SkyHanniMod.feature.rift.area.colosseumConfig
    private val entityList = mutableListOf<EntityOtherPlayerMP>()
    private val blobberName = "Blobbercyst "

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!event.isMod(5)) return
        Minecraft.getMinecraft().theWorld.loadedEntityList.filterIsInstance<EntityOtherPlayerMP>().forEach {
            if (it.name == blobberName) {
                RenderLivingEntityHelper.setEntityColor(it, Color.RED.withAlpha(80)) { isEnabled() }
                RenderLivingEntityHelper.setNoHurtTime(it) { isEnabled() }
                entityList.add(it)
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        if (!isEnabled()) return
        entityList.clear()
    }

    @SubscribeEvent
    fun onLivingDeath(event: LivingDeathEvent) {
        if (!isEnabled()) return
        if (entityList.contains(event.entity)) {
            entityList.remove(event.entity)
        }
    }

    fun isEnabled() = RiftAPI.inRift() && config.highlightBlobbercysts && LorenzUtils.skyBlockArea == "Colosseum"
}