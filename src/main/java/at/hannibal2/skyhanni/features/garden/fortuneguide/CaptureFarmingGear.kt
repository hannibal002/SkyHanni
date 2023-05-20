package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import net.minecraft.client.Minecraft
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


class CaptureFarmingGear {
    private val farmingArmor = arrayListOf("FERMENTO_BOOTS", "FERMENTO_CHESTPLATE", "FERMENTO_HELMET",
        "FERMENTO_LEGGINGS", "RANCHERS_BOOTS", "PUMPKIN_BOOTS", "SQUASH_BOOTS", "SQUASH_CHESTPLATE",
        "SQUASH_HELMET", "SQUASH_LEGGINGS", "CROPIE_BOOTS", "CROPIE_CHESTPLATE", "CROPIE_HELMET",
        "CROPIE_LEGGINGS", "MELON_BOOTS", "MELON_CHESTPLATE", "MELON_HELMET", "MELON_LEGGINGS",
        "ENCHANTED_JACK_O_LANTERN")
    //probably a way to clean this up e.g. Cropie_ + boots/helm/legs/chest
    //need to add the lower tier farming armor

    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        val hidden = GardenAPI.config?.fortune?.farmingItems ?: return
        val resultList = mutableListOf<String>()

        val itemStack = Minecraft.getMinecraft().thePlayer.inventory.getCurrentItem() ?: return
        val itemID = itemStack.getInternalName()
        resultList.add(itemStack.displayName.toString())
        resultList.add(itemID)

        val currentCrop = CropType.values().firstOrNull { itemID.startsWith(it.toolName) }
        if (currentCrop == null) {
            // could save a generic tool here eg. If they dont have a wheat hoe, use advanced garden hoe or rookie hoe
        } else {
            hidden[currentCrop.ordinal] = NEUItems.saveNBTData(itemStack)
        }
        var i = 0
        for (item in Minecraft.getMinecraft().thePlayer.inventory.armorInventory) {
            i += 1
            if (item == null) continue
            if (item.getInternalName() in farmingArmor) hidden[9 + i] = NEUItems.saveNBTData(item)
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!LorenzUtils.inSkyBlock) return
        val hidden = GardenAPI.config?.fortune?.farmingItems ?: return
        if (event.inventoryName == "Your Equipment and Stats") {
            // will not update if they equip something new, also won't
            if (event.inventoryItems[10]?.getInternalName() == "LOTUS_NECKLACE") hidden[14] = event.inventoryItems[10]?.let { NEUItems.saveNBTData(it) }
            if (event.inventoryItems[19]?.getInternalName() == "LOTUS_CLOAK") hidden[15] = event.inventoryItems[19]?.let { NEUItems.saveNBTData(it) }
            if (event.inventoryItems[28]?.getInternalName() == "LOTUS_BELT") hidden[16] = event.inventoryItems[28]?.let { NEUItems.saveNBTData(it) }
            if (event.inventoryItems[37]?.getInternalName() == "LOTUS_BRACELET") hidden[17] = event.inventoryItems[37]?.let { NEUItems.saveNBTData(it) }
        }
        if (event.inventoryName.contains("Pets")) {
            // will not detect pets like rabbit that have a yellow/green bandana currently, not a big priority
            // multiple of the same pet will cause it to be overwritten
            for (slot in event.inventoryItems) {
                if (slot.value.getInternalName().contains("ELEPHANT")) {
                    hidden[18] = NEUItems.saveNBTData(slot.value)
                }
                if (slot.value.getInternalName().contains("MOOSHROOM_COW")) {
                    hidden[19] = NEUItems.saveNBTData(slot.value)
                }
                if (slot.value.getInternalName().contains("RABBIT")) {
                    hidden[20] = NEUItems.saveNBTData(slot.value)
                }
            }
        }
    }

    //save event and populate empty
    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        val hidden = GardenAPI.config?.fortune ?: return
        while (hidden.farmingItems.size < 21) { // 10 tools, 4 armor, 4 equipment, 2 pets

            hidden.farmingItems.add(NEUItems.saveNBTData(ItemStack(Items.painting, 1, 10)))
        }
    }
}

