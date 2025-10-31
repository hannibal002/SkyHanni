package at.hannibal2.hanni.features.rift.area.colosseum

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.events.entity.EntityDeathEvent
import at.hannibal2.hanni.events.minecraft.HanniTickEvent
import at.hannibal2.hanni.features.rift.RiftApi
import at.hannibal2.hanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ColorUtils.addAlpha
import at.hannibal2.hanni.utils.EntityUtils
import at.hannibal2.hanni.utils.SkyBlockUtils
import net.minecraft.client.entity.EntityOtherPlayerMP
import java.awt.Color

@HanniModule
object BlobbercystsHighlight {

    private val config get() = HanniMod.feature.rift.area.colosseum
    private val entityList = mutableListOf<EntityOtherPlayerMP>()
    private const val BLOBBER_NAME = "Blobbercyst "

    @HandleEvent
    fun onTick(event: HanniTickEvent) {
        if (!isEnabled()) return
        if (!event.isMod(5)) return
        val color = Color.RED.addAlpha(80)
        for (player in EntityUtils.getEntities<EntityOtherPlayerMP>()) {
            if (player.name == BLOBBER_NAME) {
                RenderLivingEntityHelper.setEntityColorWithNoHurtTime(player, color) { isEnabled() }
                entityList.add(player)
            }
        }
    }

    @HandleEvent
    fun onWorldChange() {
        if (!isEnabled()) return
        entityList.clear()
    }

    @HandleEvent
    fun onEntityDeath(event: EntityDeathEvent<*>) {
        if (!isEnabled()) return
        if (entityList.contains(event.entity)) {
            entityList.remove(event.entity)
        }
    }

    fun isEnabled() = RiftApi.inRift() && config.highlightBlobbercysts && SkyBlockUtils.graphArea == "Colosseum"

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(9, "rift.area.colosseumConfig", "rift.area.colosseum")
    }
}
