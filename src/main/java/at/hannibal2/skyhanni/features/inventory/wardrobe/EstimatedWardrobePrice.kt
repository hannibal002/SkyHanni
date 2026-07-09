package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.events.minecraft.addAll
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.indexOfFirstOrNull
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.network.chat.Component

@SkyHanniModule
object EstimatedWardrobePrice {

    private val config get() = SkyHanniMod.feature.inventory.estimatedItemValues

    /**
     * REGEX-TEST: Click to equip!
     * REGEX-TEST: Click to unequip!
     */
    private val clickToEquipPattern by RepoPattern.pattern(
        "inventory.wardrobe.clicktoequip",
        "Click to (?:un)?equip!"
    )

    @HandleEvent
    fun onToolTip(event: ToolTipTextEvent) {
        if (!isEnabled()) return
        event.slot ?: return

        // TODO if we are in eq wardrobe get eq slots instead of normal wardrobe slots
        val slot = WardrobeApi.slots.firstOrNull {
            event.slot.index == it.inventorySlot && it.isInCurrentPage()
        } ?: return

        val lore = WardrobeApi.createPriceLore(slot)
        if (lore.isEmpty()) return


        val tooltip = event.toolTip
        val index = getClickToEquipIndex(tooltip)

        try {
            if (index != null) {
                tooltip.removeAt(index)
                tooltip.addAll(index, lore)
            } else {
                tooltip.addAll(lore)
            }
        } catch (e: IndexOutOfBoundsException) {
            ErrorManager.logErrorStateWithData(
                "Can not show Estimated Wardrobe Price",
                "IndexOutOfBoundsException while trying to add the estimated wardrobe price line to the tooltip",
                "index" to index,
                "lore" to lore,
            )
        }
    }

    private fun getClickToEquipIndex(tooltip: MutableList<Component>): Int? =
        tooltip.indexOfFirstOrNull { clickToEquipPattern.matches(it.string.removeColor()) }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock &&
        (config.armor && WardrobeApi.inWardrobe()) || (config.equipment && WardrobeApi.inEquipmentWardrobe()) &&
        (!WardrobeApi.inCustomWardrobe || CustomWardrobe.editMode)

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "misc.estimatedIemValueArmor", "misc.estimatedItemValues.armor")
    }
}
