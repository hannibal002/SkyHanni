package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.PetAPI
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.garden.FarmingFortuneDisplay
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.GardenAPI.getCropType
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.RegexUtils.matchFirst
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.round
import kotlin.time.Duration.Companion.days

object CaptureFarmingGear {
    private val farmingItems get() = GardenAPI.storage?.fortune?.farmingItems
    private val outdatedItems get() = GardenAPI.storage?.fortune?.outdatedItems

    private val patternGroup = RepoPattern.group("garden.fortuneguide.capture")
    private val farmingLevelUpPattern by patternGroup.pattern(
        "farminglevel",
        "SKILL LEVEL UP Farming .*➜(?<level>.*)"
    )
    private val fortuneUpgradePattern by patternGroup.pattern(
        "fortuneupgrade",
        "You claimed the Garden Farming Fortune (?<level>.*) upgrade!"
    )
    private val anitaBuffPattern by patternGroup.pattern(
        "anitabuff",
        "You tiered up the Extra Farming Drops upgrade to [+](?<level>.*)%!"
    )
    private val anitaMenuPattern by patternGroup.pattern(
        "anitamenu",
        "§7You have: §6\\+(?<level>.*)☘ Farming Fortune"
    )
    private val lotusUpgradePattern by patternGroup.pattern(
        "lotusupgrade",
        "Lotus (?<piece>.*) upgraded to [+].*☘!"
    )
    private val petLevelUpPattern by patternGroup.pattern(
        "petlevelup",
        "Your (?<pet>.*) leveled up to level .*!"
    )
    private val cakePattern by patternGroup.pattern(
        "cake",
        "(?:Big )?Yum! You (?:gain|refresh) [+]5☘ Farming Fortune for 48 hours!"
    )
    private val strengthPattern by patternGroup.pattern(
        "strength",
        " Strength: §r§c❁(?<strength>.*)"
    )

    private val tierPattern by patternGroup.pattern(
        "uniquevisitors.tier",
        "§7Progress to Tier (?<nextTier>\\w+):.*"
    )
    private val tierProgressPattern by patternGroup.pattern(
        "uniquevisitors.tierprogress",
        ".* §e(?<having>.*)§6/(?<total>.*)"
    )

    private val farmingSets = arrayListOf(
        "FERMENTO", "SQUASH", "CROPIE", "MELON", "FARM",
        "RANCHERS", "FARMER", "RABBIT"
    )

    // TODO upadte armor on equpment/wardeobe update as well
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

        TabListData.getTabList().matchFirst(strengthPattern) {
            GardenAPI.storage?.fortune?.farmingStrength = group("strength").toInt()
        }
    }

    fun reverseCarrotFortune() {
        val storage = GardenAPI.storage?.fortune ?: return
        storage.carrotFortune = !storage.carrotFortune
        ChatUtils.chat("Toggled exportable carrot fortune to: ${storage.carrotFortune}")
    }

    fun reversePumpkinFortune() {
        val storage = GardenAPI.storage?.fortune ?: return
        storage.pumpkinFortune = !storage.pumpkinFortune
        ChatUtils.chat("Toggled expired pumpkin fortune to: ${storage.pumpkinFortune}")
    }

    fun reverseCocoaBeansFortune() {
        val storage = GardenAPI.storage?.fortune ?: return
        storage.cocoaBeansFortune = !storage.cocoaBeansFortune
        ChatUtils.chat("Toggled supreme chocolate bar fortune to: ${storage.cocoaBeansFortune}")
    }

    private fun getUniqueVisitorsForTier(tier: Int): Int {
        return when {
            tier == 0 -> 0
            tier == 1 -> 1
            tier == 2 -> 5
            tier >= 3 -> 10 * (tier - 2)
            else -> throw IllegalStateException("Unexpected unique visitors tier: $tier")
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
        val items = event.inventoryItems
        if (PetAPI.isPetMenu(event.inventoryName)) {
            pets(farmingItems, items, outdatedItems)
            return
        }
        when (event.inventoryName) {
            "Your Equipment and Stats" -> equipmentAndStats(items, farmingItems, outdatedItems)
            "Your Skills" -> skills(items, storage)
            "Community Shop" -> communityShop(items)
            "Configure Plots" -> configurePlots(items, storage)
            "Anita" -> anita(items, storage)
            "Visitor Milestones" -> visitorMilestones(items)
        }
    }

    private fun visitorMilestones(items: Map<Int, ItemStack>) {
        for ((_, item) in items) {
            if (item.displayName != "§aUnique Visitors Served") continue

            var tier = -1
            var tierProgress = -1
            for (line in item.getLore()) {
                tierPattern.matchMatcher(line) {
                    tier = group("nextTier").romanToDecimalIfNecessary() - 1
                }
                tierProgressPattern.matchMatcher(line) {
                    tierProgress = group("having").toInt()
                }
            }
            if (tier > -1 && tierProgress > -1) {
                GardenAPI.storage?.uniqueVisitors = getUniqueVisitorsForTier(tier) + tierProgress
            }
        }
    }

    private fun anita(
        items: Map<Int, ItemStack>,
        storage: ProfileSpecificStorage.GardenStorage.Fortune,
    ) {
        var level = -1
        for ((_, item) in items) {
            if (item.displayName.contains("Extra Farming Fortune")) {
                level = 0

                item.getLore().matchFirst(anitaMenuPattern) {
                    level = group("level").toInt() / 4
                }
            }
        }
        if (level == -1) {
            storage.anitaUpgrade = 15
        } else {
            storage.anitaUpgrade = level
        }
    }

    private fun configurePlots(
        items: Map<Int, ItemStack>,
        storage: ProfileSpecificStorage.GardenStorage.Fortune,
    ) {
        var plotsUnlocked = 24
        for (slot in items) {
            if (slot.value.getLore().contains("§7Cost:")) {
                plotsUnlocked -= 1
            }
        }
        storage.plotsUnlocked = plotsUnlocked
    }

    private fun communityShop(items: Map<Int, ItemStack>) {
        for ((_, item) in items) {
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

    private fun skills(
        items: Map<Int, ItemStack>,
        storage: ProfileSpecificStorage.GardenStorage.Fortune,
    ) {
        for ((_, item) in items) {
            if (item.displayName.contains("Farming ")) {
                storage.farmingLevel = item.displayName.split(" ").last().romanToDecimalIfNecessary()
            }
        }
    }

    private fun pets(
        farmingItems: MutableMap<FarmingItems, ItemStack>,
        items: Map<Int, ItemStack>,
        outdatedItems: MutableMap<FarmingItems, Boolean>,
    ) {
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

        for ((_, item) in items) {
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

    private fun equipmentAndStats(
        items: Map<Int, ItemStack>,
        farmingItems: MutableMap<FarmingItems, ItemStack>,
        outdatedItems: MutableMap<FarmingItems, Boolean>,
    ) {
        for ((_, slot) in items) {
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
                val visitors = FarmingFortuneDisplay.greenThumbFortune / (greenThumbLvl * 0.05)
                GardenAPI.storage?.uniqueVisitors = round(visitors).toInt()
            }
        }
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
            storage.farmingLevel = group("level").romanToDecimalIfNecessary()
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
            val pet = group("pet").uppercase().replace("✦", "").trim().replace(" ", "_")
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
        if (msg == "CHOCOLATE BARS EXPORTATION COMPLETE!") {
            storage.cocoaBeansFortune = true
        }
        if (msg == "[NPC] Carrolyn: Thank you for the carrots.") {
            storage.carrotFortune = true
            ChatUtils.chat("§aYou have already given Carrolyn enough Exportable Carrots.")
        }
        if (msg == "[NPC] Carrolyn: Thank you for the pumpkins.") {
            storage.pumpkinFortune = true
            ChatUtils.chat("§aYou have already given Carrolyn enough Expired Pumpkins.")
        }
        if (msg == "[NPC] Carrolyn: Thank you for the chocolate.") {
            storage.cocoaBeansFortune = true
            ChatUtils.chat("§aYou have already given Carrolyn enough Supreme Chocolate Bars.")
        }
    }
}
