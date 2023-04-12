package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.InventoryCloseEvent
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
import net.minecraftforge.client.event.GuiScreenEvent
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
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val groups = accessoryBagNamePattern.matchEntire(event.inventoryName)?.groups ?: return
        isLoadingAccessories = true
        accessoryBagPageCount = groups[2]!!.value.toInt()
        accessoryBagPageNumber = groups[1]!!.value.toInt()
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
