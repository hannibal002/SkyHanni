package at.hannibal2.skyhanni.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils.round
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchFirst
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.LinkedList
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round

object ChocolateFactoryDataLoader {

    private val config get() = ChocolateFactoryAPI.config
    private val profileStorage get() = ChocolateFactoryAPI.profileStorage

    private val chocolatePerSecondPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "chocolate.persecond", "§6(?<amount>[\\d.,]+) §8per second"
    )
    private val chocolateAllTimePattern by ChocolateFactoryAPI.patternGroup.pattern(
        "chocolate.alltime", "§7All-time Chocolate: §6(?<amount>[\\d,]+)"
    )
    private val prestigeLevelPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "prestige.level", "§6Chocolate Factory (?<prestige>[IVX]+)"
    )
    private val chocolateThisPrestigePattern by ChocolateFactoryAPI.patternGroup.pattern(
        "chocolate.thisprestige", "§7Chocolate this Prestige: §6(?<amount>[\\d,]+)"
    )
    private val chocolateForPrestigePattern by ChocolateFactoryAPI.patternGroup.pattern(
        "chocolate.forprestige", "§7§cRequires (?<amount>\\w+) Chocolate this.*"
    )
    private val chocolateMultiplierPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "chocolate.multiplier", "§7Total Multiplier: §6(?<amount>[\\d.]+)x"
    )
    private val leaderboardPlacePattern by ChocolateFactoryAPI.patternGroup.pattern(
        "leaderboard.place", "§7You are §8#§b(?<position>[\\d,]+)"
    )
    private val leaderboardPercentilePattern by ChocolateFactoryAPI.patternGroup.pattern(
        "leaderboard.percentile", "§7§8You are in the top §.(?<percent>[\\d.]+)%§8 of players!"
    )
    private val barnAmountPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "barn.amount", "§7Your Barn: §.(?<rabbits>\\d+)§7/§.(?<max>\\d+) Rabbits"
    )
    private val timeTowerAmountPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "timetower.amount", "§7Charges: §.(?<uses>\\d+)§7/§a(?<max>\\d+)"
    )
    private val timeTowerStatusPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "timetower.status", "§7Status: §.§l(?<status>INACTIVE|ACTIVE)(?: §f)?(?<acitveTime>\\w*)"
    )
    private val timeTowerRechargePattern by ChocolateFactoryAPI.patternGroup.pattern(
        "timetower.recharge", "§7Next Charge: §a(?<duration>\\w+)"
    )
    private val clickMeRabbitPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "rabbit.clickme", "§e§lCLICK ME!"
    )
    private val rabbitAmountPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "rabbit.amount", "Rabbit \\S+ - \\[(?<amount>\\d+)].*"
    )
    private val upgradeTierPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "upgradetier", ".*\\s(?<tier>[IVXLC]+)"
    )
    private val unemployedRabbitPattern by ChocolateFactoryAPI.patternGroup.pattern(
        "rabbit.unemployed", "Rabbit \\w+ - Unemployed"
    )
    private val otherUpgradePattern by ChocolateFactoryAPI.patternGroup.pattern(
        "other.upgrade", "Rabbit Shrine|Coach Jackrabbit"
    )

    @SubscribeEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!ChocolateFactoryAPI.inChocolateFactory) return

        updateInventoryItems(event.inventoryItems)
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
        ChocolateFactoryAPI.inChocolateFactory = false
        ChocolateFactoryAPI.chocolateFactoryPaused = false
        ChocolateFactoryAPI.factoryUpgrades = emptyList()
        ChocolateFactoryAPI.bestAffordableSlot = -1
        ChocolateFactoryAPI.bestPossibleSlot = -1

        ChocolateFactoryAPI.allBestPossibleUpgrades.clear()
        ChocolateFactoryAPI.totalBaseIncreaseAfterUpgrades = 0
        ChocolateFactoryAPI.totalMultiplierIncreaseAfterUpgrades = 0.0
        ChocolateFactoryAPI.totalUpgradeCost = 0
    }

    fun updateInventoryItems(inventory: Map<Int, ItemStack>) {
        val profileStorage = profileStorage ?: return

        val chocolateItem = InventoryUtils.getItemAtSlotIndex(ChocolateFactoryAPI.infoIndex) ?: return
        val prestigeItem = InventoryUtils.getItemAtSlotIndex(ChocolateFactoryAPI.prestigeIndex) ?: return
        val timeTowerItem = InventoryUtils.getItemAtSlotIndex(ChocolateFactoryAPI.timeTowerIndex) ?: return
        val productionInfoItem = InventoryUtils.getItemAtSlotIndex(ChocolateFactoryAPI.productionInfoIndex) ?: return
        val leaderboardItem = InventoryUtils.getItemAtSlotIndex(ChocolateFactoryAPI.leaderboardIndex) ?: return
        val barnItem = InventoryUtils.getItemAtSlotIndex(ChocolateFactoryAPI.barnIndex) ?: return

        ChocolateFactoryAPI.factoryUpgrades = emptyList()

        processChocolateItem(chocolateItem)
        val list = mutableListOf<ChocolateFactoryUpgrade>()
        processPrestigeItem(list, prestigeItem)
        processTimeTowerItem(timeTowerItem)
        processProductionItem(productionInfoItem)
        processLeaderboardItem(leaderboardItem)
        processBarnItem(barnItem)

        profileStorage.rawChocPerSecond =
            (ChocolateFactoryAPI.chocolatePerSecond / profileStorage.chocolateMultiplier + .01).toInt()
        profileStorage.lastDataSave = SimpleTimeMark.now().toMillis()

        ChocolateFactoryStats.updateDisplay()

        processInventory(list, inventory)

        findBestUpgrades(list)
        findAllBestUpgrades(list)

        ChocolateFactoryAPI.factoryUpgrades = list
    }

    private fun processChocolateItem(item: ItemStack) {
        val profileStorage = profileStorage ?: return

        ChocolateFactoryAPI.chocolateAmountPattern.matchMatcher(item.name.removeColor()) {
            profileStorage.currentChocolate = group("amount").formatLong()
        }
        for (line in item.getLore()) {
            chocolatePerSecondPattern.matchMatcher(line) {
                ChocolateFactoryAPI.chocolatePerSecond = group("amount").formatDouble()
            }
            chocolateAllTimePattern.matchMatcher(line) {
                profileStorage.chocolateAllTime = group("amount").formatLong()
            }
        }
    }

    private fun processPrestigeItem(list: MutableList<ChocolateFactoryUpgrade>, item: ItemStack) {
        val profileStorage = profileStorage ?: return

        prestigeLevelPattern.matchMatcher(item.name) {
            ChocolateFactoryAPI.currentPrestige = group("prestige").romanToDecimal()
        }
        var prestigeCost: Long? = null
        for (line in item.getLore()) {
            chocolateThisPrestigePattern.matchMatcher(line) {
                profileStorage.chocolateThisPrestige = group("amount").formatLong()
            }
            chocolateForPrestigePattern.matchMatcher(line) {
                ChocolateFactoryAPI.chocolateForPrestige = group("amount").formatLong()
                prestigeCost = ChocolateFactoryAPI.chocolateForPrestige
            }
        }
        val prestigeUpgrade = ChocolateFactoryUpgrade(
            ChocolateFactoryAPI.prestigeIndex, ChocolateFactoryAPI.currentPrestige, prestigeCost, isPrestige = true
        )
        list.add(prestigeUpgrade)
    }

    private fun processProductionItem(item: ItemStack) {
        val profileStorage = profileStorage ?: return

        item.getLore().matchFirst(chocolateMultiplierPattern) {
            val currentMultiplier = group("amount").formatDouble()
            profileStorage.chocolateMultiplier = currentMultiplier

            if (ChocolateFactoryTimeTowerManager.timeTowerActive()) {
                profileStorage.rawChocolateMultiplier = currentMultiplier - profileStorage.timeTowerLevel * 0.1
            } else {
                profileStorage.rawChocolateMultiplier = currentMultiplier
            }
        }
    }

    private fun processLeaderboardItem(item: ItemStack) {
        ChocolateFactoryAPI.leaderboardPosition = null
        ChocolateFactoryAPI.leaderboardPercentile = null

        for (line in item.getLore()) {
            leaderboardPlacePattern.matchMatcher(line) {
                ChocolateFactoryAPI.leaderboardPosition = group("position").formatInt()
            }
            leaderboardPercentilePattern.matchMatcher(line) {
                ChocolateFactoryAPI.leaderboardPercentile = group("percent").formatDouble()
            }
        }
    }

    private fun processBarnItem(item: ItemStack) {
        val profileStorage = profileStorage ?: return

        item.getLore().matchFirst(barnAmountPattern) {
            profileStorage.currentRabbits = group("rabbits").formatInt()
            profileStorage.maxRabbits = group("max").formatInt()
            ChocolateFactoryBarnManager.trySendBarnFullMessage()
        }
    }

    private fun processTimeTowerItem(item: ItemStack) {
        val profileStorage = profileStorage ?: return

        for (line in item.getLore()) {
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
                } else {
                    profileStorage.currentTimeTowerEnds = 0
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
    }

    private fun processInventory(list: MutableList<ChocolateFactoryUpgrade>, inventory: Map<Int, ItemStack>) {
        ChocolateFactoryAPI.clickRabbitSlot = null

        for ((slotIndex, item) in inventory) {
            processItem(list, item, slotIndex)
        }
    }

    private fun processItem(list: MutableList<ChocolateFactoryUpgrade>, item: ItemStack, slotIndex: Int) {
        if (slotIndex == ChocolateFactoryAPI.prestigeIndex) return
        if (config.rabbitWarning && clickMeRabbitPattern.matches(item.name)) {
            SoundUtils.playBeepSound()
            ChocolateFactoryAPI.clickRabbitSlot = slotIndex
        }

        if (slotIndex !in ChocolateFactoryAPI.otherUpgradeSlots && slotIndex !in ChocolateFactoryAPI.rabbitSlots) return

        val itemName = item.name.removeColor()
        val lore = item.getLore()
        val upgradeCost = ChocolateFactoryAPI.getChocolateBuyCost(lore)

        val averageChocolate = ChocolateAmount.averageChocPerSecond().round(2)
        val isMaxed = upgradeCost == null

        var isRabbit = false
        var level: Int? = null
        var newAverageChocolate: Double? = null

        when (slotIndex) {
            in ChocolateFactoryAPI.rabbitSlots -> {
                level = rabbitAmountPattern.matchMatcher(itemName) {
                    group("amount").formatInt()
                } ?: run {
                    if (unemployedRabbitPattern.matches(itemName)) 0 else null
                } ?: return
                isRabbit = true

                if (isMaxed) {
                    val rabbitUpgradeItem = ChocolateFactoryUpgrade(slotIndex, level, null, isRabbit = true)
                    list.add(rabbitUpgradeItem)
                    return
                }

                val chocolateIncrease = ChocolateFactoryAPI.rabbitSlots[slotIndex] ?: 0
                newAverageChocolate = ChocolateAmount.averageChocPerSecond(rawPerSecondIncrease = chocolateIncrease)
            }

            in ChocolateFactoryAPI.otherUpgradeSlots -> {
                level = upgradeTierPattern.matchMatcher(itemName) {
                    group("tier").romanToDecimal()
                } ?: run {
                    if (otherUpgradePattern.matches(itemName)) 0 else null
                } ?: return

                if (slotIndex == ChocolateFactoryAPI.timeTowerIndex) this.profileStorage?.timeTowerLevel = level

                if (isMaxed) {
                    val otherUpgrade = ChocolateFactoryUpgrade(slotIndex, level, null)
                    list.add(otherUpgrade)
                    return
                }

                newAverageChocolate = when (slotIndex) {
                    ChocolateFactoryAPI.timeTowerIndex -> ChocolateAmount.averageChocPerSecond(
                        includeTower = true
                    )

                    ChocolateFactoryAPI.coachRabbitIndex -> ChocolateAmount.averageChocPerSecond(
                        baseMultiplierIncrease = 0.01
                    )

                    else -> {
                        val otherUpgrade = ChocolateFactoryUpgrade(slotIndex, level, upgradeCost)
                        list.add(otherUpgrade)
                        return
                    }
                }
            }
        }
        if (level == null || newAverageChocolate == null || upgradeCost == null) return

        val extra = (newAverageChocolate - averageChocolate).round(2)
        val effectiveCost = (upgradeCost / extra).round(2)

        val upgrade = ChocolateFactoryUpgrade(slotIndex, level, upgradeCost, extra, effectiveCost, isRabbit = isRabbit)
        list.add(upgrade)
    }

    private fun findAllBestUpgrades(list: List<ChocolateFactoryUpgrade>) {

        ChocolateFactoryAPI.allBestPossibleUpgrades.clear()
        ChocolateFactoryAPI.totalUpgradeCost = 0L
        for (upgrade in list) {
            ChocolateFactoryAPI.allBestPossibleUpgrades[upgrade.slotIndex] = LinkedList()
        }

        // For now only look at rabbits, implementing rabbit coach could be more complex
        val currentUpgrades = ArrayList(list)
        findAllBestUpgradesImpl(currentUpgrades, profileStorage?.currentChocolate ?: 0)

        ChocolateFactoryAPI.totalUpgradeCost = calculateTotalUpgradeCost(ChocolateFactoryAPI.allBestPossibleUpgrades)
    }

    /** Find the best possible upgrades for the current chocolate amount.
     * Should only ever get called by [findAllBestUpgrades].
     *
     * @param list list of current upgrades
     * @param remainingChocolate remaining chocolate to spend
     */
    private fun findAllBestUpgradesImpl(list: ArrayList<ChocolateFactoryUpgrade>, remainingChocolate: Long) {

        // removing time tower here as people like to determine when to buy it themselves.
        val notMaxed =
            list.filter { !it.isMaxed && it.slotIndex != ChocolateFactoryAPI.timeTowerIndex && it.effectiveCost != null }

        // find the best current upgrade out of the current possible upgrades
        val bestUpgrade = notMaxed.minByOrNull { it.effectiveCost ?: Double.MAX_VALUE }

        //  No best upgrade (all upgrades are maxed) or cant afford best upgrade
        if (bestUpgrade == null || (bestUpgrade.price ?: Long.MAX_VALUE) > remainingChocolate) return

        // For readability
        val bIndex = bestUpgrade.slotIndex

        // Keep track of total base increase and multiplier increase after upgrades
        ChocolateFactoryAPI.totalBaseIncreaseAfterUpgrades += ChocolateFactoryAPI.rabbitSlots[bIndex] ?: 0
        ChocolateFactoryAPI.totalMultiplierIncreaseAfterUpgrades += 0.1 * (if (bIndex == ChocolateFactoryAPI.coachRabbitIndex) 1 else 0)

        // Should never throw since empty lists are added in caller method.
        ChocolateFactoryAPI.allBestPossibleUpgrades[bIndex]?.add(bestUpgrade) ?: throw IllegalStateException("Best upgrade not found in list")
        ChocolateFactoryAPI.totalUpgradeCost += bestUpgrade.price ?: throw IllegalStateException("Best upgrade price is null")

        // Calculate price of next upgrade if it isn't maxed
        var nextPrice: Long? = null

        if (bestUpgrade.level + 1 < (ChocolateFactoryAPI.maxUpgradeLevelPerPrestige[bIndex]?.get(ChocolateFactoryAPI.currentPrestige - 1) ?: 0)) {

            // Use upgrade cost per level if it exists, otherwise use the formula.
            if ((ChocolateFactoryAPI.upgradeCostPerLevel[bIndex]?.size ?: 0) > bestUpgrade.level + 1) {
                val nextRaw = ChocolateFactoryAPI.upgradeCostPerLevel[bIndex]?.get(bestUpgrade.level + 1) ?: 0
                val prestigeMultiplier = (ChocolateFactoryAPI.upgradeCostFormulaConstants[bIndex]?.get("prestige")
                    ?: 0.0) * (ChocolateFactoryAPI.currentPrestige - 1)
                nextPrice = floor(nextRaw * (1 + prestigeMultiplier)).toLong()
            } else {
                val base = ChocolateFactoryAPI.upgradeCostFormulaConstants[bIndex]?.get("base") ?: 0.0
                val multiplier = ChocolateFactoryAPI.upgradeCostFormulaConstants[bIndex]?.get("exp") ?: 0.0
                val prestigeMultiplier = (ChocolateFactoryAPI.upgradeCostFormulaConstants[bIndex]?.get("prestige")
                    ?: 0.0) * (ChocolateFactoryAPI.currentPrestige - 1)
                nextPrice = floor(round(base * multiplier.pow((bestUpgrade.level + 1)) * (1 + prestigeMultiplier))).toLong()
            }
        }

        list[list.indexOf(bestUpgrade)] = ChocolateFactoryUpgrade(
            bIndex, bestUpgrade.level + 1, nextPrice, 0.0, 0.0, bestUpgrade.isRabbit, bestUpgrade.isPrestige
        )

        // Update extra per second and effective costs for all current upgrades.
        val beforeChocPerSec = ChocolateAmount.averageChocPerSecond(
            rawPerSecondIncrease = ChocolateFactoryAPI.totalBaseIncreaseAfterUpgrades,
            baseMultiplierIncrease = ChocolateFactoryAPI.totalMultiplierIncreaseAfterUpgrades
        )
        for (i in list.indices) {
            val afterChocPerSec = ChocolateAmount.averageChocPerSecond(
                rawPerSecondIncrease = ChocolateFactoryAPI.totalBaseIncreaseAfterUpgrades + (ChocolateFactoryAPI.rabbitSlots[list[i].slotIndex]
                    ?: 0),
                baseMultiplierIncrease = (ChocolateFactoryAPI.totalMultiplierIncreaseAfterUpgrades + if (list[i].slotIndex == ChocolateFactoryAPI.coachRabbitIndex) 0.01 else 0.0)
            )

            val extra: Double = (afterChocPerSec - beforeChocPerSec).round(2)
            val effectiveCost: Double? = if (list[i].price != null) (list[i].price ?: 0) / extra else null
            list[i] = list[i].copy(extraPerSecond = extra, effectiveCost = effectiveCost)
        }

        // recursive call to find the next best upgrade after spending the cost of the best upgrade.
        findAllBestUpgradesImpl(list, remainingChocolate - (bestUpgrade.price ?: 0))
    }

    private fun calculateTotalUpgradeCost(upgrades: Map<Int, List<ChocolateFactoryUpgrade>>) =
        upgrades.values.sumOf { it.sumOf { it.price ?: 0 } }


    private fun findBestUpgrades(list: MutableList<ChocolateFactoryUpgrade>) {
        val profileStorage = profileStorage ?: return

        // removing time tower here as people like to determine when to buy it themselves
        val notMaxed =
            list.filter { !it.isMaxed && it.slotIndex != ChocolateFactoryAPI.timeTowerIndex && it.effectiveCost != null }

        val bestUpgrade = notMaxed.minByOrNull { it.effectiveCost ?: Double.MAX_VALUE }
        profileStorage.bestUpgradeAvailableAt = bestUpgrade?.canAffordAt?.toMillis() ?: 0
        profileStorage.bestUpgradeCost = bestUpgrade?.price ?: 0
        ChocolateFactoryAPI.bestPossibleSlot = bestUpgrade?.getValidUpgradeIndex() ?: -1

        val bestUpgradeLevel = bestUpgrade?.level ?: 0
        ChocolateFactoryUpgradeWarning.checkUpgradeChange(ChocolateFactoryAPI.bestPossibleSlot, bestUpgradeLevel)

        val affordAbleUpgrade = notMaxed.filter { it.canAfford() }.minByOrNull { it.effectiveCost ?: Double.MAX_VALUE }
        ChocolateFactoryAPI.bestAffordableSlot = affordAbleUpgrade?.getValidUpgradeIndex() ?: -1
    }
}

