package at.hannibal2.hanni.features.fishing

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.config.core.config.Position
import at.hannibal2.hanni.events.CheckRenderEntityEvent
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.entity.EntityEnterWorldEvent
import at.hannibal2.hanni.events.fishing.FishingBobberCastEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.RenderUtils.renderString
import at.hannibal2.hanni.utils.SkyBlockUtils
import net.minecraft.entity.item.EntityArmorStand

@HanniModule
object FishingHookDisplay {

    private val config get() = HanniMod.feature.fishing.fishingHookDisplay
    private var armorStand: EntityArmorStand? = null
    private val potentialArmorStands = mutableListOf<EntityArmorStand>()
    private val pattern = "§e§l(\\d+(\\.\\d+)?)".toPattern()

    @HandleEvent
    fun onWorldChange() {
        reset()
    }

    @HandleEvent
    fun onBobberThrow(event: FishingBobberCastEvent) {
        reset()
    }

    @HandleEvent
    fun onTick() {
        if (!isEnabled()) return

        if (armorStand == null) {
            val filter = potentialArmorStands.filter { it.hasCustomName() && it.hasCorrectName() }
            if (filter.size == 1) {
                armorStand = filter[0]
            }
        }
    }

    private fun reset() {
        potentialArmorStands.clear()
        armorStand = null
    }

    @HandleEvent
    fun onJoinWorld(event: EntityEnterWorldEvent<EntityArmorStand>) {
        if (!isEnabled()) return
        potentialArmorStands.add(event.entity)
    }

    @HandleEvent
    fun onCheckRender(event: CheckRenderEntityEvent<EntityArmorStand>) {
        if (!isEnabled()) return
        if (!config.hideArmorStand) return

        if (event.entity == armorStand) {
            event.cancel()
        }
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        val armorStand = armorStand ?: return
        if (armorStand.isDead) {
            reset()
            return
        }
        if (!armorStand.hasCustomName()) return
        val alertText = if (armorStand.name == "§c§l!!!") config.customAlertText.replace("&", "§") else armorStand.name

        config.position.renderString(alertText, posLabel = "Fishing Hook Display")
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(72, "fishing.fishingHookDisplay.position", Position::migrate)
    }

    private fun EntityArmorStand.hasCorrectName(): Boolean {
        if (name == "§c§l!!!") {
            return true
        }
        return pattern.matcher(name).matches()
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled && FishingApi.holdingRod
}
