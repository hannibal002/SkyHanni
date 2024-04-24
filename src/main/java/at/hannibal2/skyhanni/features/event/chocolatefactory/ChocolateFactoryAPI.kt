package at.hannibal2.skyhanni.features.event.chocolatefactory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.event.ChocolateFactoryConfig
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage.ChocolateFactoryStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.repo.DisabledFeaturesJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.HoppityEggLocationsJson
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.CollectionUtils.nextAfter
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
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

    val config: ChocolateFactoryConfig get() = SkyHanniMod.feature.event.chocolateFactory
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
        "'§6Chocolate Factory (?<prestige>[IVX]+)"
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
        "§7Status: §.§l(?<status>INACTIVE|ACTIVE).*"
    )
    private val timeTowerRechargePattern by patternGroup.pattern(
        "timetower.recharge",
        "§7Next Charge: §a(?<duration>\\w+)"
    )

    var rabbitSlots = mapOf<Int, Int>()
    var otherUpgradeSlots = setOf<Int>()
    var noPickblockSlots = setOf<Int>()
    var barnIndex = 34
    private var infoIndex = 13
    private var productionInfoIndex = 45
    private var prestigeIndex = 28
    var milestoneIndex = 53
    private var leaderboardIndex = 51
    private var timeTowerIndex = 39
    var maxRabbits = 395

    var inChocolateFactory = false

    var currentPrestige = 0
    var chocolateCurrent = 0L
    var chocolateAllTime = 0L
    var chocolatePerSecond = 0.0
    var chocolateThisPrestige = 0L
    var chocolateMultiplier = 1.0
    var leaderboardPosition: Int? = null
    var leaderboardPercentile: Double? = null
    var timeTowerActive = false

    val upgradeableSlots: MutableSet<Int> = mutableSetOf()
    var bestUpgrade: Int? = null
    var bestRabbitUpgrade: String? = null
    var clickRabbitSlot: Int? = null

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        if (event.inventoryName != "Chocolate Factory") return
        inChocolateFactory = true

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
        val infoItem = InventoryUtils.getItemAtSlotIndex(infoIndex) ?: return
        val prestigeItem = InventoryUtils.getItemAtSlotIndex(prestigeIndex) ?: return
        val productionInfoItem = InventoryUtils.getItemAtSlotIndex(productionInfoIndex) ?: return
        val leaderboardItem = InventoryUtils.getItemAtSlotIndex(leaderboardIndex) ?: return
        val barnItem = InventoryUtils.getItemAtSlotIndex(barnIndex) ?: return
        val timeTowerItem = InventoryUtils.getItemAtSlotIndex(timeTowerIndex) ?: return

        processInfoItems(infoItem, prestigeItem, productionInfoItem, leaderboardItem, barnItem, timeTowerItem)

        bestUpgrade = null
        upgradeableSlots.clear()
        var bestAffordableUpgradeRatio = Double.MAX_VALUE
        var bestPossibleUpgradeRatio = Double.MAX_VALUE
        clickRabbitSlot = null

        for ((slotIndex, item) in inventory) {
            if (config.rabbitWarning && clickMeRabbitPattern.matches(item.name)) {
                SoundUtils.playBeepSound()
                clickRabbitSlot = slotIndex
            }

            val lore = item.getLore()
            val upgradeCost = lore.getUpgradeCost() ?: continue

            val canAfford = upgradeCost <= chocolateCurrent
            if (canAfford) upgradeableSlots.add(slotIndex)

            if (slotIndex in rabbitSlots) {
                val chocolateIncrease = rabbitSlots[slotIndex] ?: 0
                val upgradeRatio = upgradeCost.toDouble() / chocolateIncrease

                if (canAfford && upgradeRatio < bestAffordableUpgradeRatio) {
                    bestUpgrade = slotIndex
                    bestAffordableUpgradeRatio = upgradeRatio
                }
                if (upgradeRatio < bestPossibleUpgradeRatio) {
                    bestPossibleUpgradeRatio = upgradeRatio
                    bestRabbitUpgrade = item.name
                }
            }
        }
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

        chocolateMultiplier = 1.0
        timeTowerActive = false
        leaderboardPosition = null
        leaderboardPercentile = null

        chocolateAmountPattern.matchMatcher(chocolateItem.name.removeColor()) {
            chocolateCurrent = group("amount").formatLong()
        }
        for (line in chocolateItem.getLore()) {
            chocolatePerSecondPattern.matchMatcher(line) {
                chocolatePerSecond = group("amount").formatDouble()
            }
            chocolateAllTimePattern.matchMatcher(line) {
                chocolateAllTime = group("amount").formatLong()
            }
        }
        prestigeLevelPattern.matchMatcher(prestigeItem.name) {
            currentPrestige = group("prestige").romanToDecimal()
        }
        prestigeItem.getLore().matchFirst(chocolateThisPrestigePattern) {
            chocolateThisPrestige = group("amount").formatLong()
        }
        productionItem.getLore().matchFirst(chocolateMultiplierPattern) {
            chocolateMultiplier = group("amount").formatDouble()
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
                timeTowerActive = group("status") == "ACTIVE"
            }
            timeTowerRechargePattern.matchMatcher(line) {
                // todo in future fix this issue with TimeUtils.getDuration
                val formattedGroup = group("duration").replace("h", "h ").replace("m", "m ")

                val timeUntilTower = TimeUtils.getDuration(formattedGroup)
                val nextTimeTower = SimpleTimeMark.now() + timeUntilTower
                profileStorage.nextTimeTower = nextTimeTower.toMillis()
            }
        }

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
        timeTowerIndex = data.timeTowerIndex
        maxRabbits = data.maxRabbits

        val disabledFeatures = event.getConstant<DisabledFeaturesJson>("DisabledFeatures")
        HOPPITY_EVENT_DISABLED = disabledFeatures.features["HOPPITY_EVENT_DISABLED"] ?: false
    }

    private var HOPPITY_EVENT_DISABLED = false

    private fun List<String>.getUpgradeCost(): Long? {
        val nextLine = this.nextAfter({ UtilsPatterns.costLinePattern.matches(it) }) ?: return null
        return chocolateAmountPattern.matchMatcher(nextLine.removeColor()) {
            group("amount").formatLong()
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled

    fun isHoppityEvent() = SkyblockSeason.getCurrentSeason() == SkyblockSeason.SPRING &&
        (LorenzUtils.isOnAlphaServer || !HOPPITY_EVENT_DISABLED)
}
