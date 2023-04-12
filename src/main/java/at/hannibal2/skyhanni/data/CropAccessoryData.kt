package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.ProfileApiDataLoadedEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.features.garden.CropAccessory
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import com.google.gson.JsonElement
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompressedStreamTools
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.io.ByteArrayInputStream
import java.util.*

class CropAccessoryData {
    private val accessoryBagNamePattern = "Accessory Bag \\((\\d)/(\\d)\\)".toRegex()
    private var loadedAccessoryThisProfile = false
    private var ticks = 0
    private var accessoryInBag: CropAccessory? = null
    private var accessoryInInventory = CropAccessory.NONE

    private var accessoryBagPageNumber = 0

    // Handle API detection
    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        loadedAccessoryThisProfile = false
    }

    @SubscribeEvent
    fun onProfileDataLoad(event: ProfileApiDataLoadedEvent) {
        if (loadedAccessoryThisProfile) return
        val inventoryData = event.profileData["inv_contents"] ?: return
        val accessories = getCropAccessories(event.profileData["talisman_bag"]).also {
            it.addAll(getCropAccessories(inventoryData))
        }
        cropAccessory = accessories.maxOrNull() ?: CropAccessory.NONE
        loadedAccessoryThisProfile = true
    }

    // Handle accessory bag detection
    @SubscribeEvent
    fun onGuiDraw(event: InventoryOpenEvent) {
        val groups = accessoryBagNamePattern.matchEntire(event.inventoryName)?.groups ?: return
        accessoryBagPageCount = groups[2]!!.value.toInt()
        accessoryBagPageNumber = groups[1]!!.value.toInt()
        isLoadingAccessories = true

        val bestCropAccessoryPage = bestCropAccessory(event.inventoryItems.values)
        accessoryPage[accessoryBagPageNumber] = bestCropAccessoryPage
        if (accessoryBagPageCount == accessoryPage.size) {
            accessoryInBag = accessoryPage.values.max().also {
                cropAccessory = maxOf(it, accessoryInInventory)
            }
            loadedAccessoryThisProfile = true
        }
    }

    @SubscribeEvent
    fun onCloseWindow(event: GuiContainerEvent.CloseWindowEvent) {
        isLoadingAccessories = false
    }

    // Handle inventory detection
    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || ticks++ % 20 != 0) return
        if (!LorenzUtils.inSkyBlock) return
        accessoryInInventory = bestCropAccessory(InventoryUtils.getItemsInOwnInventory())
        if (accessoryInInventory == CropAccessory.NONE) return
        if (accessoryInInventory > (accessoryInBag ?: CropAccessory.NONE)) {
            cropAccessory = accessoryInInventory
        }
    }


    private fun bestCropAccessory(items: Iterable<ItemStack>): CropAccessory {
        return items.mapNotNull { item -> CropAccessory.getByName(item.getInternalName()) }
            .maxOrNull() ?: CropAccessory.NONE
    }

    companion object {
        var accessoryBagPageCount = 0
            private set

        private var accessoryPage = mutableMapOf<Int, CropAccessory>()

        var isLoadingAccessories = false
            private set

        val pagesLoaded get() = accessoryPage.size

        var cropAccessory: CropAccessory?
            get() = SkyHanniMod.feature.hidden.savedCropAccessory
            private set(accessory) {
                SkyHanniMod.feature.hidden.savedCropAccessory = accessory
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
