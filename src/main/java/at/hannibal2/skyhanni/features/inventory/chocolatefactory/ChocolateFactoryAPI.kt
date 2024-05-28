package at.hannibal2.skyhanni.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.inventory.chocolatefactory.ChocolateFactoryConfig
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage.ChocolateFactoryStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.repo.HoppityEggLocationsJson
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.event.hoppity.HoppityEggLocator
import at.hannibal2.skyhanni.utils.CollectionUtils.nextAfter
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyblockSeason
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.UtilsPatterns
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object ChocolateFactoryAPI {

    val config: ChocolateFactoryConfig get() = SkyHanniMod.feature.inventory.chocolateFactory
    val profileStorage: ChocolateFactoryStorage? get() = ProfileStorageData.profileSpecific?.chocolateFactory

    val patternGroup = RepoPattern.group("misc.chocolatefactory")
    val chocolateAmountPattern by patternGroup.pattern(
        "chocolate.amount",
        "(?<amount>[\\d,]+) Chocolate"
    )
    private val chocolateFactoryInventoryNamePattern by patternGroup.pattern(
        "inventory.name",
        "Hoppity|Chocolate Factory Milestones"
    )

    var rabbitSlots = mapOf<Int, Int>()
    var otherUpgradeSlots = setOf<Int>()
    var noPickblockSlots = setOf<Int>()
    var barnIndex = 34
    var infoIndex = 13
    var productionInfoIndex = 45
    var prestigeIndex = 28
    var milestoneIndex = 53
    var leaderboardIndex = 51
    var handCookieIndex = 38
    var timeTowerIndex = 39
    var shrineIndex = 41
    var coachRabbitIndex = 42
    var maxRabbits = 395
    private var maxPrestige = 5

    var inChocolateFactory = false
    var chocolateFactoryPaused = false

    var currentPrestige = 1
    var chocolatePerSecond = 0.0
    var leaderboardPosition: Int? = null
    var leaderboardPercentile: Double? = null
    var chocolateForPrestige = 150_000_000L

    var clickRabbitSlot: Int? = null

    var factoryUpgrades = listOf<ChocolateFactoryUpgrade>()
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

        factoryUpgrades = emptyList()
        DelayedRun.runNextTick {
            ChocolateFactoryDataLoader.updateInventoryItems(event.inventoryItems)
        }
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
        maxPrestige = data.maxPrestige

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

    fun isMaxPrestige() = currentPrestige >= maxPrestige

    fun timeUntilNeed(goal: Long): Duration {
        var needed = goal
        val profileStorage = profileStorage ?: return Duration.ZERO

        val baseMultiplier = profileStorage.rawChocolateMultiplier
        val rawChocolatePerSecond = profileStorage.rawChocPerSecond
        var timeTowerMultiplier = baseMultiplier + profileStorage.timeTowerLevel * 0.1
        if (profileStorage.hasMuRabbit) timeTowerMultiplier += 0.7

        if (rawChocolatePerSecond == 0) return Duration.INFINITE

        val secondsUntilTowerExpires = ChocolateFactoryTimeTowerManager.timeTowerActiveDuration().inWholeSeconds

        val timeTowerChocPerSecond = rawChocolatePerSecond * timeTowerMultiplier

        val secondsAtRate = needed / timeTowerChocPerSecond
        if (secondsAtRate < secondsUntilTowerExpires) {
            return secondsAtRate.seconds
        }

        needed -= (secondsUntilTowerExpires * timeTowerChocPerSecond).toLong()
        val basePerSecond = rawChocolatePerSecond * baseMultiplier
        return (needed / basePerSecond + secondsUntilTowerExpires).seconds
    }
}
