package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.data.ProfileStorageData
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
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.Minecraft
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


class CaptureFarmingGear {
    private val farmingArmor = arrayListOf(
        "FERMENTO_BOOTS", "FERMENTO_CHESTPLATE", "FERMENTO_HELMET",
        "FERMENTO_LEGGINGS", "RANCHERS_BOOTS", "PUMPKIN_BOOTS", "SQUASH_BOOTS", "SQUASH_CHESTPLATE",
        "SQUASH_HELMET", "SQUASH_LEGGINGS", "CROPIE_BOOTS", "CROPIE_CHESTPLATE", "CROPIE_HELMET",
        "CROPIE_LEGGINGS", "MELON_BOOTS", "MELON_CHESTPLATE", "MELON_HELMET", "MELON_LEGGINGS",
        "ENCHANTED_JACK_O_LANTERN"
    )
    //need to add the lower tiers of farming armor eg. pumpkin or whatever

    private val fortuneUpgradePattern = "You claimed the Garden Farming Fortune (?<level>.*) upgrade!".toPattern()

    // will not capture the user levelling up to Farming 1
    private val farmingLevelUpPattern = "SKILL LEVEL UP Farming .*➜(?<level>.*)".toPattern()
    private val anitaBuffPattern = "You tiered up the Extra Farming Drops upgrade to [+](?<level>.*)%!".toPattern()
    private val anitaMenuPattern = "§7You have: §a[+](?<level>.*)%".toPattern()

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
            // could save a generic tool here e.g. If they don't have a wheat hoe, use advanced garden hoe or rookie hoe
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
            // will not update if they equip something new
            if (event.inventoryItems[10]?.getInternalName() == "LOTUS_NECKLACE") hidden.farmingItems[14] = event.inventoryItems[10]?.let { NEUItems.saveNBTData(it) }
            if (event.inventoryItems[19]?.getInternalName() == "LOTUS_CLOAK") hidden.farmingItems[15] = event.inventoryItems[19]?.let { NEUItems.saveNBTData(it) }
            if (event.inventoryItems[28]?.getInternalName() == "LOTUS_BELT") hidden.farmingItems[16] = event.inventoryItems[28]?.let { NEUItems.saveNBTData(it) }
            if (event.inventoryItems[37]?.getInternalName() == "LOTUS_BRACELET") hidden.farmingItems[17] = event.inventoryItems[37]?.let { NEUItems.saveNBTData(it) }
        }
        if (event.inventoryName.contains("Pets")) {
            // multiple of the same pet will cause it to be overwritten
            for ((_, item) in event.inventoryItems) {
                if (item.getInternalName().contains("ELEPHANT")) {
                    hidden.farmingItems[18] = NEUItems.saveNBTData(item)
                }
                if (item.getInternalName().contains("MOOSHROOM_COW")) {
                    hidden.farmingItems[19] = NEUItems.saveNBTData(item)
                }
                if (item.getInternalName().contains("RABBIT")) {
                    hidden.farmingItems[20] = NEUItems.saveNBTData(item)
                }
            }
        }
        if (event.inventoryName.contains("Your Skills")) {
            for ((_, item) in event.inventoryItems) {
                if (item.displayName.contains("Farming ")) {
                    hidden.farmingLevel = item.displayName.split(" ").last().romanToDecimal()
                }
            }
        }
        if (event.inventoryName.contains("Community Shop")) {
            for ((_, item) in event.inventoryItems) {
                if (item.displayName.contains("Garden Farming Fortune")) {
                    if (item.getLore().contains("§aMaxed out!")) {
                        ProfileStorageData.playerSpecific?.gardenCommunityUpgrade =
                            item.displayName.split(" ").last().romanToDecimal()
                    } else {
                        ProfileStorageData.playerSpecific?.gardenCommunityUpgrade =
                            item.displayName.split(" ").last().romanToDecimal() - 1
                    }
                }
            }
        }
        if (event.inventoryName.contains("Configure Plots")) {
            hidden.plotsUnlocked = 24 - InventoryUtils.countItemsInOpenChest(true) { it.getLore().contains("§7Cost:") }
        }
        if (event.inventoryName.contains("Anita")) {
            for ((_, item) in event.inventoryItems) {
                if (item.displayName.contains("§eExtra Farming Drops")) {
                    for (line in item.getLore()) {
                        hidden.anitaUpgrade = anitaMenuPattern.matchMatcher(line) {
                            group("level").toInt() / 2
                        } ?: -1
                    }
                }
            }
        }

        println(event.inventoryName)
        for (item in event.inventoryItems) {
            println("at: ${item.key}, name: ${item.value.displayName}, internal name: ${item.value.getInternalName()}, lore: ${item.value.getLore()}")
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return
        val hidden = GardenAPI.config?.fortune ?: return
        val msg = event.message.removeColor().trim()
        fortuneUpgradePattern.matchMatcher(msg) {
            ProfileStorageData.playerSpecific?.gardenCommunityUpgrade = group("level").romanToDecimal()
        }
        farmingLevelUpPattern.matchMatcher(msg) {
            hidden.farmingLevel = group("level").romanToDecimal()
        }
        anitaBuffPattern.matchMatcher(msg) {
            hidden.anitaUpgrade = group("level").toInt() / 2
        }
        if (msg == "Yum! You gain +5☘ Farming Fortune for 48 hours!") {
            hidden.cakeExpiring = System.currentTimeMillis() + 172800000
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

