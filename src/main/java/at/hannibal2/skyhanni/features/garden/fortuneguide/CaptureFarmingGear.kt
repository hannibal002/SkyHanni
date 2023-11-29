package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.config.Storage
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
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.round
import kotlin.time.Duration.Companion.days

class CaptureFarmingGear {
    private val farmingItems get() = GardenAPI.storage?.fortune?.farmingItems
    private val outdatedItems get() = GardenAPI.storage?.fortune?.outdatedItems

    // TODO USE SH-REPO
    private val farmingLevelUpPattern = "SKILL LEVEL UP Farming .*➜(?<level>.*)".toPattern()
    private val fortuneUpgradePattern = "You claimed the Garden Farming Fortune (?<level>.*) upgrade!".toPattern()
    private val anitaBuffPattern = "You tiered up the Extra Farming Drops upgrade to [+](?<level>.*)%!".toPattern()
    private val anitaMenuPattern = "§7You have: §6\\+(?<level>.*)☘ Farming Fortune".toPattern()

    private val lotusUpgradePattern = "Lotus (?<piece>.*) upgraded to [+].*☘!".toPattern()
    private val petLevelUpPattern = "Your (?<pet>.*) leveled up to level .*!".toPattern()

    private val cakePattern = "(?:Big )?Yum! You (?:gain|refresh) [+]5☘ Farming Fortune for 48 hours!".toPattern()

    companion object {
        private val strengthPattern = " Strength: §r§c❁(?<strength>.*)".toPattern()
        private val farmingSets = arrayListOf(
            "FERMENTO", "SQUASH", "CROPIE", "MELON", "FARM",
            "RANCHERS", "FARMER", "RABBIT"
        )
        private val farmingItems get() = GardenAPI.storage?.fortune?.farmingItems

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
                    GardenAPI.storage?.fortune?.farmingStrength = group("strength").toInt()
                }
            }
        }

        fun reverseCarrotFortune() {
            val storage = GardenAPI.storage?.fortune ?: return
            storage.carrotFortune = !storage.carrotFortune
            LorenzUtils.chat("Toggled exportable carrot fortune to: ${storage.carrotFortune}")
        }

        fun reversePumpkinFortune() {
            val storage = GardenAPI.storage?.fortune ?: return
            storage.pumpkinFortune = !storage.pumpkinFortune
            LorenzUtils.chat("Toggled expired pumpkin fortune to: ${storage.pumpkinFortune}")
        }
    }

    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        captureFarmingGear()
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        val storage = GardenAPI.storage?.fortune ?: return
        val farmingItems = farmingItems ?: return
        val outdatedItems = outdatedItems ?: return
        val invName = event.inventoryName
        when {
            invName == "Your Equipment and Stats" -> getEquipmentAndStatInfo(event, farmingItems, outdatedItems)
            invName.contains("Pets") -> getPetInfo(farmingItems, event, outdatedItems)
            invName.contains("Your Skills") -> getSkillInfo(event, storage)
            invName.contains("Community Shop") -> getCommunityShopInfo(event)
            invName.contains("Configure Plots") -> getPlotInfo(event, storage)
            invName.contains("Anita") -> getAnitaInfo(event, storage)
        }
    }

    private fun getEquipmentAndStatInfo(
        event: InventoryFullyOpenedEvent,
        farmingItems: MutableMap<FarmingItems, ItemStack>,
        outdatedItems: MutableMap<FarmingItems, Boolean>
    ) {
        event.inventoryItems.values.forEach { slot ->
            val split = slot.getInternalName().asString().split("_")
            if (split.first() == "LOTUS") {
                FarmingItems.entries.find { it.name == split.last() }?.let { item ->
                    farmingItems[item] = slot
                    outdatedItems[item] = false

                    FarmingFortuneDisplay.loadFortuneLineData(slot, 0.0)
                    slot.getEnchantments()?.get("green_thumb")?.let { greenThumbLvl ->
                        val visitors = FarmingFortuneDisplay.greenThumbFortune / (greenThumbLvl * 0.05)
                        GardenAPI.storage?.uniqueVisitors = round(visitors).toInt()
                    }
                }
            }
        }
    }

    private fun getPetInfo(
        farmingItems: MutableMap<FarmingItems, ItemStack>,
        event: InventoryFullyOpenedEvent,
        outdatedItems: MutableMap<FarmingItems, Boolean>
    ) {
        val pets = listOf(FarmingItems.ELEPHANT, FarmingItems.MOOSHROOM_COW, FarmingItems.RABBIT, FarmingItems.BEE)

        // Initialize or update pets with fallback if they're null
        pets.forEach { pet -> farmingItems.putIfAbsent(pet, FFGuideGUI.getFallbackItem(pet)) }

        // Map to keep track of the highest rarity per pet,
        // setting current lvl to -1 to stop later pages saving low-rarity pets
        val highestRarityMap = pets.associateWith {
            (farmingItems[it]?.getItemRarityOrNull()?.id ?: -1) - 1
        }.toMutableMap()

        event.inventoryItems.values.forEach { item ->
            val (petType, rarityStr) = item.getInternalName().asString().split(";").let { it[0] to it[1].toInt() }
            val pet = FarmingItems.valueOf(petType)

            highestRarityMap[pet]?.let { highestRarity ->
                if (pet in pets && rarityStr > highestRarity) {
                    farmingItems[pet] = item
                    outdatedItems[pet] = false
                    highestRarityMap[pet] = rarityStr
                }
            }
        }
    }

    private fun getSkillInfo(
        event: InventoryFullyOpenedEvent,
        storage: Storage.ProfileSpecific.GardenStorage.Fortune
    ) {
        event.inventoryItems.values.firstOrNull { item ->
            item.displayName.contains("Farming ")
        }?.displayName?.split(" ")?.last()?.romanToDecimalIfNeeded()?.let { storage.farmingLevel = it }
    }

    private fun getCommunityShopInfo(event: InventoryFullyOpenedEvent) {
        event.inventoryItems.values.firstOrNull { it.displayName.contains("Garden Farming Fortune") }?.let { item ->
            val upgradeLevel = item.displayName.split(" ").last().romanToDecimal()
            ProfileStorageData.playerSpecific?.gardenCommunityUpgrade =
                if (item.getLore().contains("§aMaxed out!")) upgradeLevel else upgradeLevel - 1
        }
    }


    private fun getPlotInfo(
        event: InventoryFullyOpenedEvent,
        storage: Storage.ProfileSpecific.GardenStorage.Fortune
    ) {
        var plotsUnlocked = 24
        for (slot in event.inventoryItems) {
            if (slot.value.getLore().contains("§7Cost:")) {
                plotsUnlocked -= 1
            }
        }
        storage.plotsUnlocked = plotsUnlocked
    }

    private fun getAnitaInfo(
        event: InventoryFullyOpenedEvent,
        storage: Storage.ProfileSpecific.GardenStorage.Fortune
    ) {
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
        storage.anitaUpgrade = if (level == -1) 15 else level
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return
        val storage = GardenAPI.storage?.fortune ?: return
        val outdatedItems = outdatedItems ?: return
        val msg = event.message.removeColor().trim()
        fortuneUpgradePattern.matchMatcher(msg) {
            ProfileStorageData.playerSpecific?.gardenCommunityUpgrade = group("level").romanToDecimal()
        }
        farmingLevelUpPattern.matchMatcher(msg) {
            storage.farmingLevel = group("level").romanToDecimalIfNeeded()
        }
        anitaBuffPattern.matchMatcher(msg) {
            storage.anitaUpgrade = group("level").toInt() / 4
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
        cakePattern.matchMatcher(msg) {
            storage.cakeExpiring = System.currentTimeMillis() + 2.days.inWholeMilliseconds
        }
        if (msg == "CARROTS EXPORTATION COMPLETE!") {
            storage.carrotFortune = true
        }
        if (msg == "PUMPKINS EXPORTATION COMPLETE!") {
            storage.pumpkinFortune = true
        }
    }
}
