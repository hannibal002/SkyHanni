package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.events.minecraft.add
import at.hannibal2.skyhanni.events.minecraft.addAll
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.SkyBlockUtils

@SkyHanniModule
object EstimatedWardrobePrice {

    private val config get() = SkyHanniMod.feature.inventory.estimatedItemValues

    @HandleEvent
    fun onToolTip(event: ToolTipTextEvent) {
        if (!isEnabled()) return
        event.slot ?: return

        val api = activeWardrobeApi() ?: return
        val slot = api.slots.firstOrNull {
            event.slot.index == it.inventorySlot && it.isInCurrentPage()
        } ?: return

        val lore = api.createPriceLore(slot)
        if (lore.isEmpty()) return

        val tooltip = event.toolTip

        try {
            tooltip.add("")
            tooltip.addAll(lore)
        } catch (e: IndexOutOfBoundsException) {
            ErrorManager.logErrorStateWithData(
                "Can not show Estimated Wardrobe Price",
                "IndexOutOfBoundsException while trying to add the estimated wardrobe price line to the tooltip",
                "lore" to lore,
            )
        }
    }

    private fun activeWardrobeApi(): WardrobeApi? = when {
        config.armor && ArmorWardrobeApi.inWardrobe() -> ArmorWardrobeApi
        config.equipment && EquipmentWardrobeApi.inWardrobe() -> EquipmentWardrobeApi
        else -> null
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock &&
        (config.armor && ArmorWardrobeApi.inWardrobe()) || (config.equipment && EquipmentWardrobeApi.inWardrobe()) &&
        (!ArmorWardrobeApi.inCustomWardrobe || CustomWardrobe.editMode)

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "misc.estimatedIemValueArmor", "misc.estimatedItemValues.armor")
    }
}
