package at.hannibal2.hanni.features.hunting

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.minecraft.HanniTickEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.EntityUtils
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemCategory
import at.hannibal2.hanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.hanni.utils.RenderUtils.renderRenderable
import at.hannibal2.hanni.utils.compat.MinecraftCompat.isLocalPlayer
import at.hannibal2.hanni.utils.compat.formattedTextCompat
import at.hannibal2.hanni.utils.renderables.Renderable
import at.hannibal2.hanni.utils.renderables.primitives.text
import at.hannibal2.hanni.utils.toLorenzVec
import net.minecraft.entity.EntityLiving
import net.minecraft.entity.item.EntityArmorStand

@HanniModule
object LassoDisplay {

    private val config get() = HanniMod.feature.hunting
    private var display: Renderable? = null

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.lassoDisplay) return
        if (display == null) return
        config.lassoDisplayPosition.renderRenderable(display, posLabel = "Lasso Display")
    }

    @HandleEvent(HanniTickEvent::class, onlyOnSkyblock = true)
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
