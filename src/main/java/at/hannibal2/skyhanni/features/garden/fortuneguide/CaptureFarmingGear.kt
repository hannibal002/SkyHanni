package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.garden.FarmingFortuneDisplay
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.GardenAPI.getCropType
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNeeded
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TabListData
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.round

class CaptureFarmingGear {
    private val farmingItems get() = GardenAPI.config?.fortune?.farmingItems
    private val outdatedItems get() = GardenAPI.config?.fortune?.outdatedItems
    // TODO USE SH-REPO
    private val farmingLevelUpPattern = "SKILL LEVEL UP Farming .*➜(?<level>.*)".toPattern()
    private val fortuneUpgradePattern = "You claimed the Garden Farming Fortune (?<level>.*) upgrade!".toPattern()
    private val anitaBuffPattern = "You tiered up the Extra Farming Drops upgrade to [+](?<level>.*)%!".toPattern()
    private val anitaMenuPattern = "§7You have: §6\\+(?<level>.*)☘ Farming Fortune".toPattern()

    private val lotusUpgradePattern = "Lotus (?<piece>.*) upgraded to [+].*☘!".toPattern()
    private val petLevelUpPattern = "Your (?<pet>.*) leveled up to level .*!".toPattern()

    companion object {
        private val strengthPattern = " Strength: §r§c❁(?<strength>.*)".toPattern()
        private val farmingSets = arrayListOf("FERMENTO", "SQUASH", "CROPIE", "MELON", "FARM",
            "RANCHERS", "FARMER", "RABBIT")
        private val farmingItems get() = GardenAPI.config?.fortune?.farmingItems

        fun captureFarmingGear() {
            val farmingItems = farmingItems ?: return
            val itemStack = InventoryUtils.getItemInHand() ?: return

            val currentCrop = itemStack.getCropType()

            if (currentCrop == null) {
                //todo better fall back items
                //todo Daedalus axe
            } else {
                for (item in FarmingItems.entries) {
                    if (item.name == currentCrop.name) {
                        farmingItems[item] = itemStack
                    }
                }
            }
            for (armor in InventoryUtils.getArmor()) {
                if (armor == null) continue
                val split = armor.getInternalName().asString().split("_")
                if (split.first() in farmingSets) {
                    for (item in FarmingItems.entries) {
                        if (item.name == split.last()) {
                            farmingItems[item] = armor
                        }
                    }
                }
            }
            for (line in TabListData.getTabList()) {
                strengthPattern.matchMatcher(line) {
                    GardenAPI.config?.fortune?.farmingStrength = group("strength").toInt()
                }
            }
        }

        fun reverseCarrotFortune() {
            val hidden = GardenAPI.config?.fortune ?: return
            hidden.carrotFortune = !hidden.carrotFortune
            LorenzUtils.chat("§2Toggled exportable carrot fortune to: ${hidden.carrotFortune}")
        }
    }

    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        captureFarmingGear()
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        val hidden = GardenAPI.config?.fortune ?: return
        val farmingItems = farmingItems ?: return
        val outdatedItems = outdatedItems ?: return
        if (event.inventoryName == "Your Equipment and Stats") {
            for ((_, slot) in event.inventoryItems) {
                val split = slot.getInternalName().asString().split("_")
                if (split.first() == "LOTUS") {
                    for (item in FarmingItems.entries) {
                        if (item.name == split.last()) {
                            farmingItems[item] = slot
                            outdatedItems[item] = false
                        }
                    }
                    FarmingFortuneDisplay.loadFortuneLineData(slot, 0.0)
                    val enchantments = slot.getEnchantments() ?: emptyMap()
                    val greenThumbLvl = (enchantments["green_thumb"] ?: continue)
                    GardenAPI.config?.uniqueVisitors = round(FarmingFortuneDisplay.greenThumbFortune / (greenThumbLvl * 0.05)).toInt()
                }
            }
        }
        if (event.inventoryName.contains("Pets")) {
            // If they've 2 of same pet, one will be overwritten
            // optimize

            for (pet in listOf(
                FarmingItems.ELEPHANT,
                FarmingItems.MOOSHROOM_COW,
                FarmingItems.RABBIT,
                FarmingItems.BEE
            )) {
                if (farmingItems[pet] == null) {
                    farmingItems[pet] = FFGuideGUI.getFallbackItem(pet)
                }
            }

            // setting to current saved level -1 to stop later pages saving low rarity pets
            var highestElephantRarity = (farmingItems[FarmingItems.ELEPHANT]?.getItemRarityOrNull()?.id ?: -1) - 1
            var highestMooshroomRarity = (farmingItems[FarmingItems.MOOSHROOM_COW]?.getItemRarityOrNull()?.id ?: -1) - 1
            var highestRabbitRarity = (farmingItems[FarmingItems.RABBIT]?.getItemRarityOrNull()?.id ?: -1) - 1
            var highestBeeRarity = (farmingItems[FarmingItems.BEE]?.getItemRarityOrNull()?.id ?: -1) - 1

            for ((_, item) in event.inventoryItems) {
                val split = item.getInternalName().asString().split(";")
                if (split.first() == "ELEPHANT" && split.last().toInt() > highestElephantRarity) {
                    farmingItems[FarmingItems.ELEPHANT] = item
                    outdatedItems[FarmingItems.ELEPHANT] = false
                    highestElephantRarity = split.last().toInt()
                }
                if (split.first() == "MOOSHROOM_COW" && split.last().toInt() > highestMooshroomRarity) {
                    farmingItems[FarmingItems.MOOSHROOM_COW] = item
                    outdatedItems[FarmingItems.MOOSHROOM_COW] = false
                    highestMooshroomRarity = split.last().toInt()
                }
                if (split.first() == "RABBIT" && split.last().toInt() > highestRabbitRarity) {
                    farmingItems[FarmingItems.RABBIT] = item
                    outdatedItems[FarmingItems.RABBIT] = false
                    highestRabbitRarity = split.last().toInt()
                }
                if (split.first() == "BEE" && split.last().toInt() > highestBeeRarity) {
                        farmingItems[FarmingItems.BEE] = item
                        outdatedItems[FarmingItems.BEE] = false
                        highestBeeRarity = split.last().toInt()
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
                if (item.displayName.contains("Extra Farming Fortune")) {
                    level = 0
                    for (line in item.getLore()) {
                        anitaMenuPattern.matchMatcher(line) {
                            level = group("level").toInt() / 4
                        }
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
        val outdatedItems = outdatedItems ?: return
        val msg = event.message.removeColor().trim()
        fortuneUpgradePattern.matchMatcher(msg) {
            ProfileStorageData.playerSpecific?.gardenCommunityUpgrade = group("level").romanToDecimal()
        }
        farmingLevelUpPattern.matchMatcher(msg) {
            hidden.farmingLevel = group("level").romanToDecimalIfNeeded()
        }
        anitaBuffPattern.matchMatcher(msg) {
            hidden.anitaUpgrade = group("level").toInt() / 4
        }
        lotusUpgradePattern.matchMatcher(msg) {
            val piece = group("piece").uppercase()
            for (item in FarmingItems.entries) {
                if (item.name == piece) {
                    outdatedItems[item] = true
                }
            }
        }
        petLevelUpPattern.matchMatcher(msg) {
            val pet = group("pet").uppercase()
            for (item in FarmingItems.entries) {
                if (item.name.contains(pet)) {
                    outdatedItems[item] = true
                }
            }
        }
        if (msg == "Yum! You gain +5☘ Farming Fortune for 48 hours!") {
            hidden.cakeExpiring = System.currentTimeMillis() + 172800000
        }
        if (msg == "CARROTS EXPORTATION COMPLETE!") {
            hidden.carrotFortune = true
        }
    }
}
