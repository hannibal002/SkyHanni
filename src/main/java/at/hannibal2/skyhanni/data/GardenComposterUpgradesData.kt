package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.data.model.ComposterUpgrade
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.composter.ComposterAPI
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNeeded
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenComposterUpgradesData {

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!GardenAPI.inGarden()) return
        if (event.inventoryName != "Composter Upgrades") return
        for (item in event.inventoryItems.values) {
            val itemName = item.name ?: continue
            val matcher = ComposterUpgrade.regex.matcher(itemName)
            if (!matcher.matches()) continue

            if (matcher.groupCount() != 0) {
                val name = matcher.group("name")
                val level = matcher.group("level")?.romanToDecimalIfNeeded() ?: 0
                val composterUpgrade = ComposterUpgrade.getByName(name)!!
                ComposterAPI.composterUpgrades?.put(composterUpgrade, level)
            }
        }
    }
}