package at.hannibal2.skyhanni.features.inventory.chocolatefactory.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.CFApi
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.CFBarnManager
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.CFStats
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.CFTimeTowerManager
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.CFUpgradeWarning
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraft.item.ItemStack

@SkyHanniModule
object CFDataLoader {

    private val config get() = CFApi.config
    private val profileStorage get() = CFApi.profileStorage

    // <editor-fold desc="Patterns">
    /**
     * REGEX-TEST: §674.15 §8per second
     */
    private val chocolatePerSecondPattern by CFApi.patternGroup.pattern(
        "chocolate.persecond",
        "§6(?<amount>[\\d.,]+) §8per second",
    )

    /**
     * REGEX-TEST: §7All-time Chocolate: §67,645,879,859
     */
    private val chocolateAllTimePattern by CFApi.patternGroup.pattern(
        "chocolate.alltime",
        "§7All-time Chocolate: §6(?<amount>[\\d,]+)",
    )

    /**
     * REGEX-TEST: §6Chocolate Factory III
     */
    private val prestigeLevelPattern by CFApi.patternGroup.pattern(
        "prestige.level",
        "§6Chocolate Factory (?<prestige>[IVX]+)",
    )

    /**
     * REGEX-TEST: §7Chocolate this Prestige: §6330,382,389
     */
    private val chocolateThisPrestigePattern by CFApi.patternGroup.pattern(
        "chocolate.thisprestige",
        "§7Chocolate this Prestige: §6(?<amount>[\\d,]+)",
    )

    /**
     * REGEX-TEST: §7Max Chocolate: §660B
     */
    private val maxChocolatePattern by CFApi.patternGroup.pattern(
        "chocolate.max",
        "§7Max Chocolate: §6(?<max>.*)",
    )

    /**
     * REGEX-TEST: §7§cRequires 4B Chocolate this Prestige!
     */
    private val chocolateForPrestigePattern by CFApi.patternGroup.pattern(
        "chocolate.forprestige",
        "§7§cRequires (?<amount>\\w+) Chocolate this.*",
    )

    /**
     * REGEX-TEST: §7Total Multiplier: §61.399x
     */
    private val chocolateMultiplierPattern by CFApi.patternGroup.pattern(
        "chocolate.multiplier",
        "§7Total Multiplier: §6(?<amount>[\\d.]+)x",
    )

    /**
     * REGEX-TEST: §7You are §8#§b114
     * REGEX-TEST: §7§7You are §8#§b5,139 §7in all-time Chocolate.
     * REGEX-TEST: §7§7You are §8#§b5,139 §7in all-time
     */
    private val leaderboardPlacePattern by CFApi.patternGroup.pattern(
        "leaderboard.place",
        "(?:§.)+You are §8#§b(?<position>[\\d,]+)(?: §7in all-time)?(?: Chocolate\\.)?",
    )

    /**
     * REGEX-TEST: §7§8You are in the top §65.06%§8 of players!
     */
    private val leaderboardPercentilePattern by CFApi.patternGroup.pattern(
        "leaderboard.percentile",
        "§7§8You are in the top §.(?<percent>[\\d.]+)%§8 of players!",
    )

    /**
     * REGEX-TEST: §7Your Barn: §a16§7/§a450 Rabbits
     */
    private val barnAmountPattern by CFApi.patternGroup.pattern(
        "barn.amount",
        "§7Your Barn: §.(?<rabbits>\\d+)§7/§.(?<max>\\d+) Rabbits",
    )

    /**
     * REGEX-TEST: §7Charges: §e2§7/§a3
     */
    private val timeTowerAmountPattern by CFApi.patternGroup.pattern(
        "timetower.amount",
        "§7Charges: §.(?<uses>\\d+)§7/§a(?<max>\\d+)",
    )

    /**
     * REGEX-TEST: §7What does it do? Nobody knows...
     */
    private val timeTowerAmountEmptyPattern by CFApi.patternGroup.pattern(
        "timetower.amount.empty",
        "§7What does it do\\? Nobody knows\\.\\.\\.",
    )

    /**
     * REGEX-TEST: §7Status: §a§lACTIVE §f59m58s
     * REGEX-TEST: §7Status: §c§lINACTIVE
     */
    private val timeTowerStatusPattern by CFApi.patternGroup.pattern(
        "timetower.status",
        "§7Status: §.§l(?<status>INACTIVE|ACTIVE)(?: §f)?(?<acitveTime>\\w*)",
    )

    /**
     * REGEX-TEST: §7Next Charge: §a7h59m58s
     */
    private val timeTowerRechargePattern by CFApi.patternGroup.pattern(
        "timetower.recharge",
        "§7Next Charge: §a(?<duration>\\w+)",
    )
    val clickMeRabbitPattern by CFApi.patternGroup.pattern(
        "rabbit.clickme",
        "§e§lCLICK ME!",
    )

    /**
     * REGEX-TEST: §6§lGolden Rabbit §8- §aStampede
     */
    val clickMeGoldenRabbitPattern by CFApi.patternGroup.pattern(
        "rabbit.clickme.golden",
        "§6§lGolden Rabbit §8- §a(?<name>.*)",
    )

    /**
     * REGEX-TEST: Rabbit Bro - [14] Employee
     */
    private val rabbitAmountPattern by CFApi.patternGroup.pattern(
        "rabbit.amount",
        "Rabbit \\S+ - \\[(?<amount>\\d+)].*",
    )

    /**
     * REGEX-TEST: Time Tower I
     */
    private val upgradeTierPattern by CFApi.patternGroup.pattern(
        "upgradetier",
        ".*\\s(?<tier>[IVXLC]+)",
    )

    /**
     * REGEX-TEST: Rabbit Bro - Unemployed
     */
    private val unemployedRabbitPattern by CFApi.patternGroup.pattern(
        "rabbit.unemployed",
        "Rabbit \\w+ - Unemployed",
    )

    /**
     * REGEX-TEST: Rabbit Shrine
     * REGEX-TEST: Coach Jackrabbit
     */
    private val otherUpgradePattern by CFApi.patternGroup.pattern(
        "other.upgrade",
        "Rabbit Shrine|Coach Jackrabbit",
    )

    /**
     * REGEX-TEST: §7Available eggs: §a0
     */
    val hitmanAvailableEggsPattern by CFApi.patternGroup.pattern(
        "hitman.availableeggs",
        "§7Available eggs: §a(?<amount>\\d+)",
    )

    /**
     * REGEX-TEST: §7Purchased slots: §a28§7/§a28
     * REGEX-TEST: §7Purchased slots: §e0§7/§a22
     */
    private val hitmanPurchasedSlotsPattern by CFApi.patternGroup.pattern(
        "hitman.purchasedslots",
        "§7Purchased slots: §.(?<amount>\\d+)§7\\/§a\\d+",
    )

    /**
     * REGEX-TEST: §7Slot cooldown: §a8m 6s
     */
    private val hitmanSingleSlotCooldownPattern by CFApi.patternGroup.pattern(
        "hitman.slotcooldown",
        "§7Slot cooldown: §a(?<duration>[\\w ]+)",
    )

    /**
     * REGEX-TEST: §7All slots in: §a8h 8m 6s
     */
    private val hitmanAllSlotsCooldownPattern by CFApi.patternGroup.pattern(
        "hitman.allslotscooldown",
        "§7All slots in: §a(?<duration>[\\w ]+)",
    )

    // </editor-fold>

    @HandleEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!CFApi.inChocolateFactory) return

        updateInventoryItems(event.inventoryItems)
    }

    @HandleEvent
    fun onWorldChange() {
        clearData()
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        clearData()
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(
            47,
            "inventory.chocolateFactory.rabbitWarning",
            "inventory.chocolateFactory.rabbitWarning.rabbitWarning",
        )
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        val soundProperty = config.rabbitWarning.specialRabbitSound
        ConditionalUtils.onToggle(soundProperty) {
            CFApi.warningSound = SoundUtils.createSound(soundProperty.get(), 1f)
        }

        config.chocolateUpgradeWarnings.upgradeWarningTimeTower.whenChanged { _, _ ->
            CFApi.factoryUpgrades.takeIf { it.isNotEmpty() }?.let {
                findBestUpgrades(it)
            } ?: run {
                ChatUtils.clickableChat(
                    "Could not determine your current statistics to get next upgrade. Open CF to fix this!",
                    onClick = { HypixelCommands.chocolateFactory() },
                    "§eClick to run /cf!",
                )
            }
        }
    }

    private fun clearData() {
        CFApi.chocolateFactoryPaused = false
        CFApi.factoryUpgrades = emptyList()
        CFApi.bestAffordableSlot = -1
        CFApi.bestPossibleSlot = -1
    }

    fun updateInventoryItems(inventory: Map<Int, ItemStack>) {
        val profileStorage = profileStorage ?: return

        val chocolateItem = InventoryUtils.getItemAtSlotIndex(CFApi.infoIndex) ?: return
        val prestigeItem = InventoryUtils.getItemAtSlotIndex(CFApi.prestigeIndex) ?: return
        val timeTowerItem = InventoryUtils.getItemAtSlotIndex(CFApi.timeTowerIndex) ?: return
        val productionInfoItem = InventoryUtils.getItemAtSlotIndex(CFApi.productionInfoIndex) ?: return
        val leaderboardItem = InventoryUtils.getItemAtSlotIndex(CFApi.leaderboardIndex) ?: return
        val barnItem = InventoryUtils.getItemAtSlotIndex(CFApi.barnIndex) ?: return
        val hitmanItem = InventoryUtils.getItemAtSlotIndex(CFApi.rabbitHitmanIndex) ?: return

        CFApi.factoryUpgrades = emptyList()

        processChocolateItem(chocolateItem)
        val list = mutableListOf<CFUpgrade>()
        processPrestigeItem(list, prestigeItem)
        processTimeTowerItem(timeTowerItem)
        processProductionItem(productionInfoItem)
        processLeaderboardItem(leaderboardItem)
        processBarnItem(barnItem)
        processHitmanItem(hitmanItem)

        profileStorage.rawChocPerSecond = (CFApi.chocolatePerSecond / profileStorage.chocolateMultiplier + .01).toInt()
        profileStorage.lastDataSave = SimpleTimeMark.now()

        CFStats.updateDisplay()

        processInventory(list, inventory)

        findBestUpgrades(list)
        CFApi.factoryUpgrades = list
    }

    private fun processChocolateItem(item: ItemStack) {
        val profileStorage = profileStorage ?: return

        CFApi.chocolateAmountPattern.matchMatcher(item.displayName.removeColor()) {
            profileStorage.currentChocolate = group("amount").formatLong()
        }
        for (line in item.getLore()) {
            chocolatePerSecondPattern.matchMatcher(line) {
                CFApi.chocolatePerSecond = group("amount").formatDouble()
            }
            chocolateAllTimePattern.matchMatcher(line) {
                profileStorage.chocolateAllTime = group("amount").formatLong()
            }
        }
    }

    private fun processPrestigeItem(list: MutableList<CFUpgrade>, item: ItemStack) {
        val profileStorage = profileStorage ?: return

        prestigeLevelPattern.matchMatcher(item.displayName) {
            CFApi.currentPrestige = group("prestige").romanToDecimal()
        }
        var prestigeCost: Long? = null
        for (line in item.getLore()) {
            chocolateThisPrestigePattern.matchMatcher(line) {
                profileStorage.chocolateThisPrestige = group("amount").formatLong()
            }
            maxChocolatePattern.matchMatcher(line) {
                profileStorage.maxChocolate = group("max").formatLong()
            }
            chocolateForPrestigePattern.matchMatcher(line) {
                CFApi.chocolateForPrestige = group("amount").formatLong()
                prestigeCost = CFApi.chocolateForPrestige
            }
        }
        val prestigeUpgrade = CFUpgrade(
            CFApi.prestigeIndex,
            CFApi.currentPrestige,
            prestigeCost,
            isPrestige = true,
        )
        list.add(prestigeUpgrade)
    }

    private fun processProductionItem(item: ItemStack) {
        val profileStorage = profileStorage ?: return

        chocolateMultiplierPattern.firstMatcher(item.getLore()) {
            val currentMultiplier = group("amount").formatDouble()
            profileStorage.chocolateMultiplier = currentMultiplier

            if (CFTimeTowerManager.timeTowerActive()) {
                profileStorage.rawChocolateMultiplier = currentMultiplier - profileStorage.timeTowerLevel * 0.1
            } else {
                profileStorage.rawChocolateMultiplier = currentMultiplier
            }
        }
    }

    private fun processLeaderboardItem(item: ItemStack) {
        CFApi.leaderboardPosition = null
        CFApi.leaderboardPercentile = null

        for (line in item.getLore()) {
            leaderboardPlacePattern.matchMatcher(line) {
                CFApi.leaderboardPosition = group("position").formatInt()
            }
            leaderboardPercentilePattern.matchMatcher(line) {
                CFApi.leaderboardPercentile = group("percent").formatDouble()
            }
        }
    }

    private fun processBarnItem(item: ItemStack) {
        val profileStorage = profileStorage ?: return

        barnAmountPattern.firstMatcher(item.getLore()) {
            profileStorage.currentRabbits = group("rabbits").formatInt()
            profileStorage.maxRabbits = group("max").formatInt()
            CFBarnManager.trySendBarnFullMessage(inventory = true)
        }
    }

    private fun processTimeTowerItem(item: ItemStack) {
        val profileStorage = profileStorage ?: return

        for (line in item.getLore()) {
            timeTowerAmountPattern.matchMatcher(line) {
                profileStorage.currentTimeTowerUses = group("uses").formatInt()
                profileStorage.maxTimeTowerUses = group("max").formatInt()
                CFTimeTowerManager.checkTimeTowerWarning(true)
            }
            if (timeTowerAmountEmptyPattern.matches(line)) {
                profileStorage.currentTimeTowerUses = 0
                profileStorage.maxTimeTowerUses = 0
                profileStorage.currentTimeTowerUses = 0
            }
            timeTowerStatusPattern.matchMatcher(line) {
                val activeTime = group("acitveTime")
                if (activeTime.isNotEmpty()) {
                    val activeUntil = SimpleTimeMark.now() + TimeUtils.getDuration(activeTime)
                    profileStorage.currentTimeTowerEnds = activeUntil
                } else {
                    profileStorage.currentTimeTowerEnds = SimpleTimeMark.farPast()
                }
            }
            timeTowerRechargePattern.matchMatcher(line) {
                val timeUntilTower = TimeUtils.getDuration(group("duration"))
                val nextTimeTower = SimpleTimeMark.now() + timeUntilTower
                profileStorage.nextTimeTower = nextTimeTower
            }
        }
    }

    private fun processHitmanItem(item: ItemStack) {
        val profileStorage = profileStorage ?: return

        for (line in item.getLore()) {
            hitmanAvailableEggsPattern.matchMatcher(line) {
                profileStorage.hitmanStats.availableHitmanEggs = group("amount").formatInt()
            }
            hitmanSingleSlotCooldownPattern.matchMatcher(line) {
                val timeUntilSlot = TimeUtils.getDuration(group("duration"))
                val nextSlot = (SimpleTimeMark.now() + timeUntilSlot)
                profileStorage.hitmanStats.singleSlotCooldownMark = nextSlot
            }
            hitmanAllSlotsCooldownPattern.matchMatcher(line) {
                val timeUntilAllSlots = TimeUtils.getDuration(group("duration"))
                val nextAllSlots = (SimpleTimeMark.now() + timeUntilAllSlots)
                profileStorage.hitmanStats.allSlotsCooldownMark = nextAllSlots
            }
            hitmanPurchasedSlotsPattern.matchMatcher(line) {
                profileStorage.hitmanStats.purchasedHitmanSlots = group("amount").formatInt()
            }
        }
    }

    private fun processInventory(list: MutableList<CFUpgrade>, inventory: Map<Int, ItemStack>) {
        for ((slotIndex, item) in inventory) {
            processItem(list, item, slotIndex)
        }
    }

    private fun processItem(list: MutableList<CFUpgrade>, item: ItemStack, slotIndex: Int) {
        if (slotIndex == CFApi.prestigeIndex) return

        if (slotIndex !in CFApi.otherUpgradeSlots && slotIndex !in CFApi.rabbitSlots) return

        val itemName = item.displayName.removeColor()
        val lore = item.getLore()
        val upgradeCost = CFApi.getChocolateBuyCost(lore)
        val averageChocolate = ChocolateAmount.averageChocPerSecond().roundTo(2)
        val isMaxed = upgradeCost == null

        if (slotIndex in CFApi.rabbitSlots) {
            handleRabbitSlot(list, itemName, slotIndex, isMaxed, upgradeCost, averageChocolate)
        } else if (slotIndex in CFApi.otherUpgradeSlots) {
            handleOtherUpgradeSlot(list, itemName, slotIndex, isMaxed, upgradeCost, averageChocolate)
        }
    }

    private fun handleRabbitSlot(
        list: MutableList<CFUpgrade>,
        itemName: String,
        slotIndex: Int,
        isMaxed: Boolean,
        upgradeCost: Long?,
        averageChocolate: Double,
    ) {
        val level = rabbitAmountPattern.matchMatcher(itemName) {
            group("amount").formatInt()
        } ?: run {
            if (unemployedRabbitPattern.matches(itemName)) 0 else null
        } ?: return

        if (isMaxed) {
            val rabbitUpgradeItem = CFUpgrade(slotIndex, level, null, isRabbit = true)
            list.add(rabbitUpgradeItem)
            return
        }

        val chocolateIncrease = CFApi.rabbitSlots[slotIndex] ?: 0
        val newAverageChocolate = ChocolateAmount.averageChocPerSecond(rawPerSecondIncrease = chocolateIncrease)
        addUpgradeToList(list, slotIndex, level, upgradeCost, averageChocolate, newAverageChocolate, isRabbit = true)
    }

    private fun handleOtherUpgradeSlot(
        list: MutableList<CFUpgrade>,
        itemName: String,
        slotIndex: Int,
        isMaxed: Boolean,
        upgradeCost: Long?,
        averageChocolate: Double,
    ) {
        val level = upgradeTierPattern.matchMatcher(itemName) {
            group("tier").romanToDecimal()
        } ?: run {
            if (otherUpgradePattern.matches(itemName)) 0 else null
        } ?: return

        if (slotIndex == CFApi.timeTowerIndex) {
            profileStorage?.timeTowerLevel = level
        }

        if (isMaxed) {
            val otherUpgrade = CFUpgrade(slotIndex, level, null)
            list.add(otherUpgrade)
            return
        }

        val newAverageChocolate = when (slotIndex) {
            CFApi.timeTowerIndex -> ChocolateAmount.averageChocPerSecond(includeTower = true)
            CFApi.coachRabbitIndex -> ChocolateAmount.averageChocPerSecond(baseMultiplierIncrease = 0.01)
            else -> {
                val otherUpgrade = CFUpgrade(slotIndex, level, upgradeCost)
                list.add(otherUpgrade)
                return
            }
        }

        addUpgradeToList(list, slotIndex, level, upgradeCost, averageChocolate, newAverageChocolate, isRabbit = false)
    }

    private fun addUpgradeToList(
        list: MutableList<CFUpgrade>,
        slotIndex: Int,
        level: Int,
        upgradeCost: Long?,
        averageChocolate: Double,
        newAverageChocolate: Double,
        isRabbit: Boolean,
    ) {
        val extra = (newAverageChocolate - averageChocolate).roundTo(2)
        val effectiveCost = ((upgradeCost ?: 0) / extra).roundTo(2)
        val upgrade = CFUpgrade(slotIndex, level, upgradeCost, extra, effectiveCost, isRabbit = isRabbit)
        list.add(upgrade)
    }

    private fun findBestUpgrades(list: List<CFUpgrade>) {
        val profileStorage = profileStorage ?: return

        val ttFiltered = list.filter {
            config.chocolateUpgradeWarnings.upgradeWarningTimeTower.get() || it.slotIndex != CFApi.timeTowerIndex
        }

        val notMaxed = ttFiltered.filter {
            !it.isMaxed && it.effectiveCost != null
        }

        val bestUpgrade = notMaxed.minByOrNull { it.effectiveCost ?: Double.MAX_VALUE }
        profileStorage.bestUpgradeAvailableAt = bestUpgrade?.canAffordAt ?: SimpleTimeMark.farPast()
        profileStorage.bestUpgradeCost = bestUpgrade?.price ?: 0
        CFApi.bestPossibleSlot = bestUpgrade?.getValidUpgradeIndex() ?: -1

        val bestUpgradeLevel = bestUpgrade?.level ?: 0
        CFUpgradeWarning.checkUpgradeChange(CFApi.bestPossibleSlot, bestUpgradeLevel)

        val affordAbleUpgrade = notMaxed.filter { it.canAfford() }.minByOrNull { it.effectiveCost ?: Double.MAX_VALUE }
        CFApi.bestAffordableSlot = affordAbleUpgrade?.getValidUpgradeIndex() ?: -1
    }
}
