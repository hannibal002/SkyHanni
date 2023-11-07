package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.features.garden.CropAccessory
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import com.google.gson.JsonElement
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompressedStreamTools
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.io.ByteArrayInputStream
import java.util.Base64

class CropAccessoryData {
    // TODO USE SH-REPO
    private val accessoryBagNamePattern = "Accessory Bag \\((\\d)/(\\d)\\)".toPattern()
    private var loadedAccessoryThisProfile = false
    private var ticks = 0
    private var accessoryInBag: CropAccessory? = null
    private var accessoryInInventory = CropAccessory.NONE

    private var accessoryBagPageNumber = 0

    // Handle API detection
    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        loadedAccessoryThisProfile = false

        accessoryPage.clear()
    }

    // Handle accessory bag detection
    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {

        // handling accessory bags with only one page
        if (event.inventoryName == "Accessory Bag") {
            isLoadingAccessories = true
            accessoryBagPageCount = 1
            accessoryBagPageNumber = 1
            return
        }

        accessoryBagNamePattern.matchMatcher(event.inventoryName) {
            isLoadingAccessories = true
            accessoryBagPageCount = group(0).toInt()
            accessoryBagPageNumber = group(1).toInt()
        } ?: return
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        isLoadingAccessories = false
    }

    @SubscribeEvent
    fun onGuiDraw(event: GuiScreenEvent.DrawScreenEvent) {
        if (!isLoadingAccessories) return
        val items = runCatching {
            InventoryUtils.getItemsInOpenChest()
        }.getOrNull() ?: return
        val bestCropAccessoryPage = bestCropAccessory(items.map { it.stack })
        accessoryPage[accessoryBagPageNumber] = bestCropAccessoryPage
        if (accessoryBagPageCount == accessoryPage.size) {
            accessoryInBag = accessoryPage.values.max().also {
                cropAccessory = maxOf(it, accessoryInInventory)
            }
            loadedAccessoryThisProfile = true
        }
    }

    // Handle inventory detection
    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!event.repeatSeconds(1)) return
        if (!LorenzUtils.inSkyBlock) return
        accessoryInInventory = bestCropAccessory(InventoryUtils.getItemsInOwnInventory())
        if (accessoryInInventory == CropAccessory.NONE) return
        if (accessoryInInventory > (accessoryInBag ?: CropAccessory.NONE)) {
            cropAccessory = accessoryInInventory
        }
    }


    private fun bestCropAccessory(items: Iterable<ItemStack>) =
        items.mapNotNull { item -> CropAccessory.getByName(item.getInternalName()) }
            .maxOrNull() ?: CropAccessory.NONE

    companion object {
        var accessoryBagPageCount = 0
            private set

        private var accessoryPage = mutableMapOf<Int, CropAccessory>()

        var isLoadingAccessories = false
            private set

        val pagesLoaded get() = accessoryPage.size

        var cropAccessory: CropAccessory?
            get() = GardenAPI.storage?.savedCropAccessory
            private set(accessory) {
                GardenAPI.storage?.savedCropAccessory = accessory
            }

        // Derived partially from NotEnoughUpdates/NotEnoughUpdates, ProfileViewer.Profile#getInventoryInfo
        private fun getCropAccessories(inventory: JsonElement?): MutableList<CropAccessory> {
            if (inventory == null) return mutableListOf()
            val cropAccessories = mutableListOf<CropAccessory>()
            val data = inventory.asJsonObject["data"]?.asString
            val accessoryBagItems = CompressedStreamTools.readCompressed(
                ByteArrayInputStream(Base64.getDecoder().decode(data))
            ).getTagList("i", 10)
            for (j in 0 until accessoryBagItems.tagCount()) {
                val itemStackTag = accessoryBagItems.getCompoundTagAt(j)
                if (!itemStackTag.hasKey("tag")) continue
                val itemTag = itemStackTag.getCompoundTag("tag")
                val itemName = NEUItems.getInternalNameOrNull(itemTag) ?: continue
                val itemAsCropAccessory = CropAccessory.getByName(itemName) ?: continue
                cropAccessories.add(itemAsCropAccessory)
            }
            return cropAccessories
        }
    }
}
