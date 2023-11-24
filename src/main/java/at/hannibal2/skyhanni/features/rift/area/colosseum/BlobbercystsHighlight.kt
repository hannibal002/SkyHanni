package at.hannibal2.skyhanni.features.rift.area.colosseum

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

class BlobbercystsHighlight {

    private val config get() = SkyHanniMod.feature.rift.area.colosseum
    private val entityList = mutableListOf<EntityOtherPlayerMP>()
    private val blobberName = "Blobbercyst "

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!event.isMod(5)) return
        EntityUtils.getEntities<EntityOtherPlayerMP>().forEach {
            if (it.name == blobberName) {
                RenderLivingEntityHelper.setEntityColor(it, Color.RED.withAlpha(80)) { isEnabled() }
                RenderLivingEntityHelper.setNoHurtTime(it) { isEnabled() }
                entityList.add(it)
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
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

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(9, "rift.area.colosseumConfig", "rift.area.colosseum")
    }
}
