package at.hannibal2.skyhanni.features.inventory.wardrobe

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.inCustomWardrobe
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.inWardrobe
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.isInCurrentPage
import at.hannibal2.skyhanni.features.inventory.wardrobe.WardrobeAPI.wardrobeSlots
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class EstimatedWardrobePrice {

    private val config get() = SkyHanniMod.feature.inventory.estimatedItemValues

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.armor) return
        if (!inWardrobe()) return
        if (inCustomWardrobe) return

        val slot = wardrobeSlots.firstOrNull {
            event.slot.slotNumber == it.inventorySlot && it.isInCurrentPage()
        } ?: return


        val lore = WardrobeAPI.createWardrobePriceLore(slot)
        if (lore.isEmpty()) return

        val tooltip = event.toolTip
        var index = 3

        tooltip.add(index++, "")
        tooltip.add(index++, "§aEstimated Armor Value:")
        lore.forEach {
            tooltip.add(index++, it)
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "misc.estimatedIemValueArmor", "misc.estimatedItemValues.armor")
    }
}
