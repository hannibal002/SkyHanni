package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.Storage.PlayerSpecific
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNeeded
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
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
    //need to add the lower tiers of farming armor eg. pumpkin or whatever

    private val fortuneUpgradePattern = "You claimed the Garden Farming Fortune (?<level>.*) upgrade!".toPattern()
    // will not capture the user levelling up to Farming 1 but also they cannot use garden at this level so doesnt matter
    private val farmingLevelUpPattern = "SKILL LEVEL UP Farming .*➜(?<level>.*)".toPattern()

    //todo anita buff
    //todo finish cakes, currently don't add the 48 hrs yet

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
        val hidden = GardenAPI.config?.fortune ?: return
        if (event.inventoryName == "Your Equipment and Stats") {
            // will not update if they equip something new, also won't
            if (event.inventoryItems[10]?.getInternalName() == "LOTUS_NECKLACE") hidden.farmingItems[14] = event.inventoryItems[10]?.let { NEUItems.saveNBTData(it) }
            if (event.inventoryItems[19]?.getInternalName() == "LOTUS_CLOAK") hidden.farmingItems[15] = event.inventoryItems[19]?.let { NEUItems.saveNBTData(it) }
            if (event.inventoryItems[28]?.getInternalName() == "LOTUS_BELT") hidden.farmingItems[16] = event.inventoryItems[28]?.let { NEUItems.saveNBTData(it) }
            if (event.inventoryItems[37]?.getInternalName() == "LOTUS_BRACELET") hidden.farmingItems[17] = event.inventoryItems[37]?.let { NEUItems.saveNBTData(it) }
        }
        if (event.inventoryName.contains("Pets")) {
            // multiple of the same pet will cause it to be overwritten
            for (slot in event.inventoryItems) {
                if (slot.value.getInternalName().contains("ELEPHANT")) {
                    hidden.farmingItems[18] = NEUItems.saveNBTData(slot.value)
                }
                if (slot.value.getInternalName().contains("MOOSHROOM_COW")) {
                    hidden.farmingItems[19] = NEUItems.saveNBTData(slot.value)
                }
                if (slot.value.getInternalName().contains("RABBIT")) {
                    hidden.farmingItems[20] = NEUItems.saveNBTData(slot.value)
                }
            }
        }
        if (event.inventoryName.contains("Your Skills")) {
            for (slot in event.inventoryItems) {
                if (slot.value.displayName.contains("Farming ")) {
                    hidden.farmingLevel = slot.value.displayName.split(" ").last().romanToDecimal()
                }
            }
        }
        if (event.inventoryName.contains("Community Shop")) {
            for (slot in event.inventoryItems) {
                if (slot.value.displayName.contains("Garden Farming Fortune")) {
                    if (slot.value.getLore().contains("§aMaxed out!")) {
                        //todo, move to player specific
                        SkyHanniMod.feature.storage.gardenCommunityUpgrade = slot.value.displayName.split(" ").last().romanToDecimal()
                    } else {
                        SkyHanniMod.feature.storage.gardenCommunityUpgrade = slot.value.displayName.split(" ").last().romanToDecimal() - 1
                    }
                }
            }
        }
        if (event.inventoryName.contains("Configure Plots")) {
            var plots = 24
            for (slot in event.inventoryItems) {
                if (slot.value.getLore().contains("§7Cost:")) {
                    plots -= 1
                }
            }
            hidden.plotsUnlocked = plots
        }

//        println(event.inventoryName)
//        for (item in event.inventoryItems) {
//            println("at: ${item.key}, name: ${item.value.displayName}, internal name: ${item.value.getInternalName()}, lore: ${item.value.getLore()}")
//        }
    }

    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return
        val hidden = GardenAPI.config?.fortune ?: return
        val msg = event.message.removeColor().trim()
        println(event.message)
        var matcher = fortuneUpgradePattern.matcher(msg)
        if (matcher.matches()) {
            SkyHanniMod.feature.storage.gardenCommunityUpgrade = matcher.group("level").romanToDecimal()
        }
        matcher = farmingLevelUpPattern.matcher(msg)
        if (matcher.matches()) {
            hidden.farmingLevel = matcher.group("level").romanToDecimal()
        }
        if (msg == "Yum! You gain +5☘ Farming Fortune for 48 hours!") {
            hidden.lastCakeTime = System.currentTimeMillis()
        }
    }


    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        val hidden = GardenAPI.config?.fortune ?: return
        while (hidden.farmingItems.size < 21) { // 10 tools, 4 armor, 4 equipment, 3 pets

            hidden.farmingItems.add(NEUItems.saveNBTData(ItemStack(Items.painting, 1, 10)))
        }
    }
}

