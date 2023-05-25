package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.GardenAPI.getCropType
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNeeded
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CaptureFarmingGear {
    private val farmingItems get() = GardenAPI.config?.fortune?.farmingItems

    private val farmingLevelUpPattern = "SKILL LEVEL UP Farming .*➜(?<level>.*)".toPattern()
    private val fortuneUpgradePattern = "You claimed the Garden Farming Fortune (?<level>.*) upgrade!".toPattern()
    private val anitaBuffPattern = "You tiered up the Extra Farming Drops upgrade to [+](?<level>.*)%!".toPattern()
    private val anitaMenuPattern = "§7You have: §a[+](?<level>.*)%".toPattern()

    companion object {
        private val farmingSets = arrayListOf("FERMENTO", "SQUASH", "CROPIE", "MELON", "FARM") // not adding any more armor, unless requested
        private val farmingBoots = arrayListOf("RANCHERS_BOOTS", "PUMPKIN_BOOTS")
        private val farmingItems get() = GardenAPI.config?.fortune?.farmingItems

        fun captureFarmingGear() {
            val farmingItems = farmingItems ?: return
            val resultList = mutableListOf<String>()

            val itemStack = Minecraft.getMinecraft().thePlayer.inventory.getCurrentItem() ?: return
            val itemID = itemStack.getInternalName()
            resultList.add(itemStack.displayName.toString())
            resultList.add(itemID)

            val currentCrop = itemStack.getCropType()

            if (currentCrop == null) {
                // could save a generic tool here e.g. If they don't have a wheat hoe, use advanced garden hoe or rookie hoe
            } else {
                for (item in FarmingItems.values()) {
                    if (item.name == currentCrop.name) {
                        farmingItems[item] = itemStack
                    }
                }
            }
            for (armor in InventoryUtils.getArmor()) {
                if (armor == null) continue
                val split = armor.getInternalName().split("_")
                if (split.first() in farmingSets) {
                    for (item in FarmingItems.values()) {
                        if (item.name == split.last()) {
                            farmingItems[item] = armor
                        }
                    }
                }
                if (armor.getInternalName() in farmingBoots) {
                    farmingItems[FarmingItems.OTHER_BOOTS] = armor
                }
            }
        }

    }

    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        captureFarmingGear()
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!LorenzUtils.inSkyBlock) return
        val hidden = GardenAPI.config?.fortune ?: return
        val farmingItems = farmingItems ?: return
        if (event.inventoryName == "Your Equipment and Stats") {
            for ((_, slot) in event.inventoryItems) {
                val split = slot.getInternalName().split("_")
                if (split.first() == "LOTUS") {
                    for (item in FarmingItems.values()) {
                        if (item.name == split.last()) {
                            farmingItems[item] = slot
                        }
                    }
                }
            }
        }
        if (event.inventoryName.contains("Pets")) {
            //todo fix multiple of the same pet will cause it to be overwritten
            for ((_, item) in event.inventoryItems) {
                val split = item.getInternalName().split(";")
                if (split.first() == "ELEPHANT") {
                    farmingItems[FarmingItems.ELEPHANT] = item
                }
                if (split.first() == "MOOSHROOM_COW") {
                    farmingItems[FarmingItems.MOOSHROOM_COW] = item
                }
                if (split.first() == "RABBIT") {
                    farmingItems[FarmingItems.RABBIT] = item
                }
            }
        }

        if (event.inventoryName.contains("Your Skills")) {
            for ((_, item) in event.inventoryItems) {
                if (item.displayName.contains("Farming ")) {
                    hidden.farmingLevel = item.displayName.split(" ").last().romanToDecimalIfNeeded()
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
            var plotsUnlocked = 24
            for (slot in event.inventoryItems) {
                if (slot.value.getLore().contains("§7Cost:")) {
                    plotsUnlocked -= 1
                }
            }
            hidden.plotsUnlocked = plotsUnlocked
        }
        if (event.inventoryName.contains("Anita")) {
            var level = -1
            for ((_, item) in event.inventoryItems) {
                if (item.displayName.contains("§eExtra Farming Drops")) {
                    for (line in item.getLore()) {
                        level = anitaMenuPattern.matchMatcher(line) {
                            group("level").toInt() / 2
                        } ?: -1
                    }
                }
            }
            if (level == -1) {
                hidden.anitaUpgrade = 15
            } else {
                hidden.anitaUpgrade = level
            }
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
            hidden.farmingLevel = group("level").romanToDecimalIfNeeded()
        }
        anitaBuffPattern.matchMatcher(msg) {
            hidden.anitaUpgrade = group("level").toInt() / 2
        }
        if (msg == "Yum! You gain +5☘ Farming Fortune for 48 hours!") {
            hidden.cakeExpiring = System.currentTimeMillis() + 172800000
        }
    }
}
