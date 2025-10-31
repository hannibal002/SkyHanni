package at.hannibal2.hanni.data

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.InventoryUpdatedEvent
import at.hannibal2.hanni.events.ProfileJoinEvent
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.features.garden.CropAccessory
import at.hannibal2.hanni.features.garden.GardenApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils.getInternalName
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack

@HanniModule
object CropAccessoryData {

    /**
     * REGEX-TEST: Accessory Bag (1/2)
     */
    private val accessoryBagNamePattern by RepoPattern.pattern(
        "data.accessory.bagname.new",
        "Accessory Bag.*",
    )

    private var accessoryInBag = CropAccessory.NONE
    private var accessoryInInventory = CropAccessory.NONE

    @HandleEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        accessoryInBag = CropAccessory.NONE
        accessoryInInventory = CropAccessory.NONE
    }

    @HandleEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!accessoryBagNamePattern.matches(event.inventoryName)) return

        val items = event.inventoryItems.values
        val bestInPage = bestCropAccessory(items)
        if (bestInPage > accessoryInBag) {
            accessoryInBag = bestInPage
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!event.repeatSeconds(5)) return

        accessoryInInventory = bestCropAccessory(InventoryUtils.getItemsInOwnInventory())

        val bestAccessory = maxOf(accessoryInInventory, accessoryInBag)
        if (bestAccessory > cropAccessory) {
            cropAccessory = bestAccessory
        }
    }

    private fun bestCropAccessory(items: Collection<ItemStack>) =
        items.mapNotNull { item -> CropAccessory.getByName(item.getInternalName()) }
            .maxOrNull() ?: CropAccessory.NONE

    var cropAccessory: CropAccessory
        get() = GardenApi.storage?.savedCropAccessory ?: CropAccessory.NONE
        private set(accessory) {
            GardenApi.storage?.savedCropAccessory = accessory
        }
}
