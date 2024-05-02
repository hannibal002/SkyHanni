package at.hannibal2.skyhanni.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.inventory.chocolatefactory.ChocolateFactoryConfig
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage.ChocolateFactoryStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.repo.HoppityEggLocationsJson
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.event.hoppity.HoppityEggLocator
import at.hannibal2.skyhanni.utils.CollectionUtils.nextAfter
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyblockSeason
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchFirst
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.UtilsPatterns
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ChocolateFactoryAPI {

    val config: ChocolateFactoryConfig get() = SkyHanniMod.feature.inventory.chocolateFactory
    val profileStorage: ChocolateFactoryStorage? get() = ProfileStorageData.profileSpecific?.chocolateFactory

    val patternGroup = RepoPattern.group("misc.chocolatefactory")
    private val chocolateAmountPattern by patternGroup.pattern(
        "chocolate.amount",
        "(?<amount>[\\d,]+) Chocolate"
    )
    private val chocolatePerSecondPattern by patternGroup.pattern(
        "chocolate.persecond",
        "§6(?<amount>[\\d.,]+) §8per second"
    )
    private val chocolateAllTimePattern by patternGroup.pattern(
        "chocolate.alltime",
        "§7All-time Chocolate: §6(?<amount>[\\d,]+)"
    )
    private val chocolateThisPrestigePattern by patternGroup.pattern(
        "chocolate.thisprestige",
        "§7Chocolate this Prestige: §6(?<amount>[\\d,]+)"
    )
    private val chocolateMultiplierPattern by patternGroup.pattern(
        "chocolate.multiplier",
        "§7Total Multiplier: §6(?<amount>[\\d.]+)x"
    )
    private val barnAmountPattern by patternGroup.pattern(
        "barn.amount",
        "§7Your Barn: §.(?<rabbits>\\d+)§7/§.(?<max>\\d+) Rabbits"
    )
    private val prestigeLevelPattern by patternGroup.pattern(
        "prestige.level",
        "§6Chocolate Factory (?<prestige>[IVX]+)"
    )
    private val chocolateForPrestigePattern by patternGroup.pattern(
        "chocolate.forprestige",
        "§7§cRequires (?<amount>\\w+) Chocolate this.*"
    )
    private val clickMeRabbitPattern by patternGroup.pattern(
        "rabbit.clickme",
        "§e§lCLICK ME!"
    )
    private val leaderboardPlacePattern by patternGroup.pattern(
        "leaderboard.place",
        "§7You are §8#§b(?<position>[\\d,]+)"
    )
    private val leaderboardPercentilePattern by patternGroup.pattern(
        "leaderboard.percentile",
        "§7§8You are in the top §.(?<percent>[\\d.]+)%§8 of players!"
    )
    private val timeTowerAmountPattern by patternGroup.pattern(
        "timetower.amount",
        "§7Charges: §.(?<uses>\\d+)§7/§a(?<max>\\d+)"
    )
    private val timeTowerStatusPattern by patternGroup.pattern(
        "timetower.status",
        "§7Status: §.§l(?<status>INACTIVE|ACTIVE)(?: §f)?(?<acitveTime>\\w*)"
    )
    private val timeTowerRechargePattern by patternGroup.pattern(
        "timetower.recharge",
        "§7Next Charge: §a(?<duration>\\w+)"
    )
    private val chocolateFactoryInventoryNamePattern by patternGroup.pattern(
        "inventory.name",
        "Hoppity|Chocolate Factory Milestones"
    )
    private val rabbitAmountPattern by patternGroup.pattern(
        "rabbit.amount",
        "Rabbit \\S+ - \\[(?<amount>\\d+)].*"
    )
    private val upgradeTierPattern by patternGroup.pattern(
        "upgradetier",
        ".*\\s(?<tier>[IVXLC]+)"
    )

    var rabbitSlots = mapOf<Int, Int>()
    var otherUpgradeSlots = setOf<Int>()
    var noPickblockSlots = setOf<Int>()
    var barnIndex = 34
    private var infoIndex = 13
    private var productionInfoIndex = 45
    var prestigeIndex = 28
    var milestoneIndex = 53
    private var leaderboardIndex = 51
    var handCookieIndex = 38
    var timeTowerIndex = 39
    var shrineIndex = 41
    var coachRabbitIndex = 42
    var maxRabbits = 395

    var inChocolateFactory = false
    var chocolateFactoryPaused = false

    var currentPrestige = 1
    var chocolatePerSecond = 0.0
    var leaderboardPosition: Int? = null
    var leaderboardPercentile: Double? = null
    var chocolateForPrestige = 150_000_000L

    var clickRabbitSlot: Int? = null

    val factoryUpgrades = mutableListOf<ChocolateFactoryUpgrade>()
    var bestAffordableSlot = -1
    var bestPossibleSlot = -1

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return

        if (chocolateFactoryInventoryNamePattern.matches(event.inventoryName)) {
            chocolateFactoryPaused = true
            ChocolateFactoryStats.updateDisplay()
            return
        }
        if (event.inventoryName != "Chocolate Factory") return
        inChocolateFactory = true

        factoryUpgrades.clear()
        DelayedRun.runNextTick {
            updateInventoryItems(event.inventoryItems)
        }
    }

    @SubscribeEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!inChocolateFactory) return

        updateInventoryItems(event.inventoryItems)
    }

    private fun updateInventoryItems(inventory: Map<Int, ItemStack>) {
        val profileStorage = profileStorage ?: return

        val infoItem = InventoryUtils.getItemAtSlotIndex(infoIndex) ?: return
        val prestigeItem = InventoryUtils.getItemAtSlotIndex(prestigeIndex) ?: return
        val productionInfoItem = InventoryUtils.getItemAtSlotIndex(productionInfoIndex) ?: return
        val leaderboardItem = InventoryUtils.getItemAtSlotIndex(leaderboardIndex) ?: return
        val barnItem = InventoryUtils.getItemAtSlotIndex(barnIndex) ?: return
        val timeTowerItem = InventoryUtils.getItemAtSlotIndex(timeTowerIndex) ?: return

        processInfoItems(infoItem, prestigeItem, productionInfoItem, leaderboardItem, barnItem, timeTowerItem)
        clickRabbitSlot = null

        for ((slotIndex, item) in inventory) {
            if (config.rabbitWarning && clickMeRabbitPattern.matches(item.name)) {
                SoundUtils.playBeepSound()
                clickRabbitSlot = slotIndex
            }

            if (slotIndex !in otherUpgradeSlots && slotIndex !in rabbitSlots) continue

            val itemName = item.name.removeColor()
            val lore = item.getLore()
            val upgradeCost = getChocolateBuyCost(lore)

            val averageChocolate = ChocolateAmount.averageChocPerSecond().round(2)
            val isMaxed = upgradeCost == null

            var isRabbit = false
            var level: Int? = null
            var newAverageChocolate: Double? = null

            when (slotIndex) {
                in rabbitSlots -> {
                    level = rabbitAmountPattern.matchMatcher(itemName) {
                        group("amount").formatInt()
                    } ?: continue

                    isRabbit = true

                    if (isMaxed) {
                        val rabbitUpgradeItem = ChocolateFactoryUpgrade(slotIndex, level, null, isRabbit = true)
                        factoryUpgrades.add(rabbitUpgradeItem)
                        continue
                    }

                    val chocolateIncrease = rabbitSlots[slotIndex] ?: 0
                    newAverageChocolate = ChocolateAmount.averageChocPerSecond(rawPerSecondIncrease = chocolateIncrease)
                }

                in otherUpgradeSlots -> {
                    level = upgradeTierPattern.matchMatcher(itemName) {
                        group("tier").romanToDecimal()
                    } ?: continue

                    if (slotIndex == timeTowerIndex) profileStorage?.timeTowerLevel = level

                    if (isMaxed) {
                        val otherUpgrade = ChocolateFactoryUpgrade(slotIndex, level, null)
                        factoryUpgrades.add(otherUpgrade)
                        continue
                    }

                    newAverageChocolate = when (slotIndex) {
                        timeTowerIndex -> ChocolateAmount.averageChocPerSecond(timeTowerLevelIncrease = 1)
                        coachRabbitIndex -> ChocolateAmount.averageChocPerSecond(baseMultiplierIncrease = 0.01)
                        else -> {
                            val otherUpgrade = ChocolateFactoryUpgrade(slotIndex, level, upgradeCost)
                            factoryUpgrades.add(otherUpgrade)
                            continue
                        }
                    }
                }
            }
            if (level == null || newAverageChocolate == null || upgradeCost == null) continue

            val extra = (newAverageChocolate - averageChocolate).round(2)
            val effectiveCost = (upgradeCost / extra).round(2)

            val upgrade =
                ChocolateFactoryUpgrade(slotIndex, level, upgradeCost, extra, effectiveCost, isRabbit = isRabbit)
            factoryUpgrades.add(upgrade)
        }

        val notMaxed = factoryUpgrades.filter { !it.isMaxed }

        val bestUpgrade = notMaxed.minByOrNull { it.effectiveCost ?: Double.MAX_VALUE }
        profileStorage.bestUpgradeAvailableAt = bestUpgrade?.canAffordAt?.toMillis() ?: 0
        bestPossibleSlot = bestUpgrade?.getValidUpgradeIndex() ?: -1

        val affordAbleUpgrade = notMaxed.filter { it.canAfford() }.minByOrNull { it.effectiveCost ?: Double.MAX_VALUE }
        bestAffordableSlot = affordAbleUpgrade?.getValidUpgradeIndex() ?: -1
    }

    private fun processInfoItems(
        chocolateItem: ItemStack,
        prestigeItem: ItemStack,
        productionItem: ItemStack,
        leaderboardItem: ItemStack,
        barnItem: ItemStack,
        timeTowerItem: ItemStack,
    ) {
        val profileStorage = profileStorage ?: return

        leaderboardPosition = null
        leaderboardPercentile = null

        chocolateAmountPattern.matchMatcher(chocolateItem.name.removeColor()) {
            profileStorage.currentChocolate = group("amount").formatLong()
        }
        for (line in chocolateItem.getLore()) {
            chocolatePerSecondPattern.matchMatcher(line) {
                chocolatePerSecond = group("amount").formatDouble()
            }
            chocolateAllTimePattern.matchMatcher(line) {
                profileStorage.chocolateAllTime = group("amount").formatLong()
            }
        }
        prestigeLevelPattern.matchMatcher(prestigeItem.name) {
            currentPrestige = group("prestige").romanToDecimal()
        }
        var prestigeCost: Long? = null
        for (line in prestigeItem.getLore()) {
            chocolateThisPrestigePattern.matchMatcher(line) {
                profileStorage.chocolateThisPrestige = group("amount").formatLong()
            }
            chocolateForPrestigePattern.matchMatcher(line) {
                chocolateForPrestige = group("amount").formatLong()
                prestigeCost = chocolateForPrestige
            }

        }
        val prestigeUpgrade = ChocolateFactoryUpgrade(prestigeIndex, currentPrestige, prestigeCost, isPrestige = true)
        factoryUpgrades.add(prestigeUpgrade)
        productionItem.getLore().matchFirst(chocolateMultiplierPattern) {
            val currentMultiplier = group("amount").formatDouble()
            profileStorage.chocolateMultiplier = currentMultiplier

            if (ChocolateFactoryTimeTowerManager.timeTowerActive()) {
                profileStorage.rawChocolateMultiplier = currentMultiplier - profileStorage.timeTowerLevel * 0.1
            } else {
                profileStorage.rawChocolateMultiplier = currentMultiplier
            }
        }
        for (line in leaderboardItem.getLore()) {
            leaderboardPlacePattern.matchMatcher(line) {
                leaderboardPosition = group("position").formatInt()
            }
            leaderboardPercentilePattern.matchMatcher(line) {
                leaderboardPercentile = group("percent").formatDouble()
            }
        }
        barnItem.getLore().matchFirst(barnAmountPattern) {
            profileStorage.currentRabbits = group("rabbits").formatInt()
            profileStorage.maxRabbits = group("max").formatInt()
            ChocolateFactoryBarnManager.trySendBarnFullMessage()
        }
        for (line in timeTowerItem.getLore()) {
            timeTowerAmountPattern.matchMatcher(line) {
                profileStorage.currentTimeTowerUses = group("uses").formatInt()
                profileStorage.maxTimeTowerUses = group("max").formatInt()
                ChocolateFactoryTimeTowerManager.checkTimeTowerWarning(true)
            }
            timeTowerStatusPattern.matchMatcher(line) {
                val activeTime = group("acitveTime")
                if (activeTime.isNotEmpty()) {
                    // todo in future fix this issue with TimeUtils.getDuration
                    val formattedGroup = activeTime.replace("h", "h ").replace("m", "m ")

                    val activeDuration = TimeUtils.getDuration(formattedGroup)
                    val activeUntil = SimpleTimeMark.now() + activeDuration
                    profileStorage.currentTimeTowerEnds = activeUntil.toMillis()
                }
            }
            timeTowerRechargePattern.matchMatcher(line) {
                // todo in future fix this issue with TimeUtils.getDuration
                val formattedGroup = group("duration").replace("h", "h ").replace("m", "m ")

                val timeUntilTower = TimeUtils.getDuration(formattedGroup)
                val nextTimeTower = SimpleTimeMark.now() + timeUntilTower
                profileStorage.nextTimeTower = nextTimeTower.toMillis()
            }
        }
        profileStorage.rawChocPerSecond = (chocolatePerSecond / profileStorage.chocolateMultiplier).toInt()
        profileStorage.lastDataSave = SimpleTimeMark.now().toMillis()

        if (!config.statsDisplay) return
        ChocolateFactoryStats.updateDisplay()
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        clearData()
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        clearData()
    }

    private fun clearData() {
        inChocolateFactory = false
        chocolateFactoryPaused = false
        factoryUpgrades.clear()
        bestAffordableSlot = -1
        bestPossibleSlot = -1
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<HoppityEggLocationsJson>("HoppityEggLocations")

        HoppityEggLocator.eggLocations = data.eggLocations

        rabbitSlots = data.rabbitSlots
        otherUpgradeSlots = data.otherUpgradeSlots
        noPickblockSlots = data.noPickblockSlots
        barnIndex = data.barnIndex
        infoIndex = data.infoIndex
        productionInfoIndex = data.productionInfoIndex
        prestigeIndex = data.prestigeIndex
        milestoneIndex = data.milestoneIndex
        leaderboardIndex = data.leaderboardIndex
        handCookieIndex = data.handCookieIndex
        timeTowerIndex = data.timeTowerIndex
        shrineIndex = data.shrineIndex
        coachRabbitIndex = data.coachRabbitIndex
        maxRabbits = data.maxRabbits

        ChocolateFactoryUpgrade.updateIgnoredSlots()
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        val old = "event.chocolateFactory"
        val new = "inventory.chocolateFactory"
        event.move(44, "$old.enabled", "$new.enabled")
        event.move(44, "$old.statsDisplay", "$new.statsDisplay")
        event.move(44, "$old.statsDisplayList", "$new.statsDisplayList")
        event.move(44, "$old.showStackSizes", "$new.showStackSizes")
        event.move(44, "$old.highlightUpgrades", "$new.highlightUpgrades")
        event.move(44, "$old.useMiddleClick", "$new.useMiddleClick")
        event.move(44, "$old.rabbitWarning", "$new.rabbitWarning")
        event.move(44, "$old.barnCapacityThreshold", "$new.barnCapacityThreshold")
        event.move(44, "$old.extraTooltipStats", "$new.extraTooltipStats")
        event.move(44, "$old.timeTowerWarning", "$new.timeTowerWarning")
        event.move(44, "$old.position", "$new.position")
        event.move(44, "$old.compactOnClick", "$new.compactOnClick")
        event.move(44, "$old.compactOnClickAlways", "$new.compactOnClickAlways")
        event.move(44, "$old.tooltipMove", "$new.tooltipMove")
        event.move(44, "$old.tooltipMovePosition", "$new.tooltipMovePosition")
        event.move(44, "$old.hoppityMenuShortcut", "$new.hoppityMenuShortcut")
        event.move(44, "$old.hoppityCollectionStats", "$new.hoppityCollectionStats")
        event.move(44, "$old.hoppityStatsPosition", "$new.hoppityStatsPosition")
    }

    fun getChocolateBuyCost(lore: List<String>): Long? {
        val nextLine = lore.nextAfter({ UtilsPatterns.costLinePattern.matches(it) }) ?: return null
        return chocolateAmountPattern.matchMatcher(nextLine.removeColor()) {
            group("amount").formatLong()
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled

    fun isHoppityEvent() = SkyblockSeason.getCurrentSeason() == SkyblockSeason.SPRING
}
