package at.hannibal2.skyhanni.features.rift.area.colosseum

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.entity.EntityDeathEvent
import at.hannibal2.skyhanni.events.entity.EntityEnterWorldEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import net.minecraft.client.entity.EntityOtherPlayerMP
import java.awt.Color

@SkyHanniModule
object BlobbercystsHighlight {

    private val config get() = SkyHanniMod.feature.rift.area.colosseum
    private val entityList = mutableSetOf<EntityOtherPlayerMP>()
    private const val BLOBBER_NAME = "Blobbercyst "

    @HandleEvent
    fun onEntityEnterWorld(event: EntityEnterWorldEvent<EntityOtherPlayerMP>) {
        if (!isEnabled()) return
        val entity = event.entity
        if (entity.name != BLOBBER_NAME) return
        RenderLivingEntityHelper.setEntityColorWithNoHurtTime(entity, Color.RED.addAlpha(80)) { isEnabled() }
        entityList.add(entity)
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
