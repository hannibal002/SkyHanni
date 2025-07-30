package at.hannibal2.skyhanni.features.rift.area.colosseum

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.entity.EntityDeathEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import net.minecraft.client.entity.EntityOtherPlayerMP
import java.awt.Color

@SkyHanniModule
object BlobbercystsHighlight {

    private val config get() = SkyHanniMod.feature.rift.area.colosseum
    private val entityList = mutableListOf<EntityOtherPlayerMP>()
    private const val BLOBBER_NAME = "Blobbercyst "

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
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
