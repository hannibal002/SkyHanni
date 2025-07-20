package at.hannibal2.skyhanni.features.hunting

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat.isLocalPlayer
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.entity.EntityLiving
import net.minecraft.entity.item.EntityArmorStand

@SkyHanniModule
object LassoDisplay {

    private val config get() = SkyHanniMod.feature.hunting
    private var display: Renderable? = null

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.lassoDisplay) return
        if (display == null) return
        config.lassoDisplayPosition.renderRenderable(display, posLabel = "Lasso Display")
    }

    @HandleEvent(SkyHanniTickEvent::class, onlyOnSkyblock = true)
    fun onTick() {
        if (!config.lassoDisplay) return
        var isReel = false
        var progressBar = ""
        if (InventoryUtils.getItemInHand()?.getItemCategoryOrNull() != ItemCategory.LASSO) {
            display = null
            return
        }
        for (entity in EntityUtils.getAllEntities()) {
            //#if MC < 1.21
            if (entity !is EntityLiving) continue
            val leashEntity = entity.leashedToEntity ?: continue
            //#else
            //$$ if (entity !is net.minecraft.entity.Leashable) continue
            //$$ val leashEntity = entity.leashHolder ?: continue
            //#endif
            if (!leashEntity.isLocalPlayer) continue
            val entitiesNearby = EntityUtils.getEntitiesNearby<EntityArmorStand>(entity.position.toLorenzVec().up(2), 2.0)
            for (armorStandEntity in entitiesNearby) {
                val name = armorStandEntity.displayName.formattedTextCompat()
                if (name.contains("§l§m")) {
                    progressBar = name
                }
                if (name.removeSuffix("§r") == "§e§lREEL") {
                    isReel = true
                    break
                }
            }
        }
        display = if (isReel) {
            Renderable.text("§e§l          REEL          ")
        } else if (progressBar.isNotEmpty()) {
            Renderable.text(progressBar)
        } else null
    }

    @HandleEvent
    fun onConfigFixEvent(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(100, "foraging.lassoDisplay", "hunting.lassoDisplay")
        event.move(100, "foraging.lassoDisplayPosition", "hunting.lassoDisplayPosition")
    }
}
