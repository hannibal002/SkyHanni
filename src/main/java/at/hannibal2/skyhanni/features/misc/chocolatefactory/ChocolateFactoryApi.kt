package at.hannibal2.skyhanni.features.misc.chocolatefactory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.misc.ChocolateFactoryConfig
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage.ChocolateFactoryStorage
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.repo.HoppityEggLocationsJson
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.utils.CollectionUtils.nextAfter
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.SkyblockSeason
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchFirst
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds

object ChocolateFactoryApi {

    val config: ChocolateFactoryConfig get() = SkyHanniMod.feature.misc.chocolateFactory
    val profileStorage: ChocolateFactoryStorage? get() = ProfileStorageData.profileSpecific?.chocolateFactory

    val patternGroup = RepoPattern.group("misc.chocolatefactory")
    private val chocolateAmountPattern by patternGroup.pattern(
        "chocolate.amount",
        "(?<chocolate>[\\d,]+) Chocolate"
    )
    private val barnAmountPattern by patternGroup.pattern(
        "barn.amount",
        "§7Your Barn: §.(?<rabbits>\\d+)§7/§.(?<max>\\d+) Rabbits"
    )
    private val rabbitDuplicatePattern by patternGroup.pattern(
        "rabbit.duplicate",
        "§7§lDUPLICATE RABBIT! §6\\+[\\d,]+ Chocolate"
    )
    private val clickMeRabbitPattern by patternGroup.pattern(
        "rabbit.clickme",
        "§e§lCLICK ME!"
    )

    private var eggLocations: Map<IslandType, List<LorenzVec>> = mapOf()

    var rabbitSlots = mapOf<Int, Int>()
    var otherUpgradeSlots = setOf<Int>()
    var noPickblockSlots = setOf<Int>()
    var barnIndex = 34
    var infoIndex = 13
    var milestoneIndex = 53
    var maxRabbits = 395

    var inChocolateFactory = false

    var currentChocolate = 0L

    val upgradeableSlots: MutableSet<Int> = mutableSetOf()
    var bestUpgrade: Int? = null
    var bestRabbitUpgrade: String? = null

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (event.inventoryName != "Chocolate Factory") return
        inChocolateFactory = true

        DelayedRun.runDelayed(50.milliseconds) {
            updateInventoryItems(event.inventoryItems)
        }
    }

    @SubscribeEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!inChocolateFactory) return

        updateInventoryItems(event.inventoryItems)
    }

    private fun updateInventoryItems(inventory: Map<Int, ItemStack>) {
        val profileStorage = profileStorage ?: return
        val chocolateItem = InventoryUtils.getItemsInOpenChest().find { it.slotIndex == infoIndex }?.stack ?: return

        chocolateAmountPattern.matchMatcher(chocolateItem.name.removeColor()) {
            currentChocolate = group("chocolate").formatLong()
        }

        bestUpgrade = null
        upgradeableSlots.clear()

        var bestAffordableUpgradeRatio = Double.MAX_VALUE
        var bestPossibleUpgradeRatio = Double.MAX_VALUE

        for ((slotIndex, item) in inventory) {
            if (config.rabbitWarning && clickMeRabbitPattern.matches(item.name)) {
                // todo new file
                println("tried to play sound")
                SoundUtils.playBeepSound()
            }

            val upgradeCost = item.getLore().getUpgradeCost() ?: continue

            if (slotIndex == barnIndex) {
                item.getLore().matchFirst(barnAmountPattern) {
                    profileStorage.currentRabbits = group("rabbits").formatInt()
                    profileStorage.maxRabbits = group("max").formatInt()

                    ChocolateFactoryBarnManager.trySendBarnFullMessage()
                }
            }

            val canAfford = upgradeCost <= currentChocolate
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

        eggLocations = data.eggLocations

        rabbitSlots = data.rabbitSlots
        otherUpgradeSlots = data.otherUpgradeSlots
        noPickblockSlots = data.noPickblockSlots
        barnIndex = data.barnIndex
        infoIndex = data.infoIndex
        milestoneIndex = data.milestoneIndex
        maxRabbits = data.maxRabbits
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        CakeMealTime.checkClaimed()
    }

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!inChocolateFactory) return
        val slot = event.slot ?: return
        if (!config.useMiddleClick) return
        if (slot.slotNumber in noPickblockSlots) return

        event.makePickblock()
    }

    fun getCurrentIslandEggLocations(): List<LorenzVec>? {
        return eggLocations[LorenzUtils.skyBlockIsland]
    }

    private fun List<String>.getUpgradeCost(): Long? {
        val nextLine = this.nextAfter({ it == "§7Cost" }) ?: return null
        chocolateAmountPattern.matchMatcher(nextLine.removeColor()) {
            return group("chocolate").formatLong()
        }
        return null
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
    fun isHoppityEvent() = SkyblockSeason.getCurrentSeason() == SkyblockSeason.SPRING
}
