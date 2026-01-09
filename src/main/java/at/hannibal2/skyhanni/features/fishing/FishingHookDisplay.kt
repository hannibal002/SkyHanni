package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.entity.EntityEnterWorldEvent
import at.hannibal2.skyhanni.events.fishing.FishingBobberCastEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.compat.deceased
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import net.minecraft.world.entity.decoration.ArmorStand

@SkyHanniModule
object FishingHookDisplay {

    private val config get() = SkyHanniMod.feature.fishing.fishingHookDisplay
    private var armorStand: ArmorStand? = null
    private val potentialArmorStands = mutableListOf<ArmorStand>()
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
    fun onJoinWorld(event: EntityEnterWorldEvent<ArmorStand>) {
        if (!isEnabled()) return
        potentialArmorStands.add(event.entity)
    }

    @HandleEvent
    fun onCheckRender(event: CheckRenderEntityEvent<ArmorStand>) {
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
        if (armorStand.deceased) {
            reset()
            return
        }
        if (!armorStand.hasCustomName()) return
        val alertText = if (armorStand.name.string == "!!!") config.customAlertText.replace("&", "§") else armorStand.name.formattedTextCompatLessResets()

        config.position.renderString(alertText, posLabel = "Fishing Hook Display")
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(72, "fishing.fishingHookDisplay.position", Position::migrate)
    }

    private fun ArmorStand.hasCorrectName(): Boolean {
        if (name.string == "!!!") {
            return true
        }
        return pattern.matcher(name.formattedTextCompatLessResets()).matches()
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled && FishingApi.holdingRod
}
