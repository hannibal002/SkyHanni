package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuCarnivalTokenCostJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuMiscJson
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.skyhanni.features.inventory.EssenceShopHelper.essenceUpgradePattern
import at.hannibal2.skyhanni.features.inventory.EssenceShopHelper.maxedUpgradeLorePattern
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ItemUtils.createItemStack
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object CarnivalShopHelper {

    // Where the informational item stack will be placed in the GUI
    private const val CUSTOM_STACK_LOCATION = 8

    private var eventShops = mutableListOf<EventShop>()
    private var currentProgress: EventShopProgress? = null
    private var currentEventType: String = ""
    private var tokensOwned: Int = 0
    private var tokensNeeded: Int = 0
    private var infoItemStack: ItemStack? = null

    /**
     * REGEX-TEST:
     */
    private val currentTokenCountPattern by patternGroup.pattern(
        "carnival.tokens.current",
        ".*§7Your Tokens: §a(?<tokens>[\\d,]*)"
    )

    data class EventShop(val shopName: String, val upgrades: List<NeuCarnivalTokenCostJson>)
    data class EventShopUpgradeStatus(
        val upgradeName: String,
        val currentLevel: Int,
        val maxLevel: Int,
        val remainingCosts: MutableList<Int>,
    )

    data class EventShopProgress(val shopName: String, val purchasedUpgrades: Map<String, Int>) {
        private val eventShop = eventShops.find { it.shopName.equals(shopName, ignoreCase = true) }
        val remainingUpgrades: MutableList<EventShopUpgradeStatus> = eventShop?.upgrades?.map {
            val purchasedAmount = purchasedUpgrades[it.name] ?: 0
            EventShopUpgradeStatus(
                it.name,
                currentLevel = purchasedAmount,
                maxLevel = it.costs.count(),
                remainingCosts = it.costs.drop(purchasedAmount).toMutableList(),
            )
        }.orEmpty().toMutableList()
        val nonRepoUpgrades = purchasedUpgrades.filter { purchasedUpgrade ->
            eventShop?.upgrades?.none { it.name.equals(purchasedUpgrade.key, ignoreCase = true) } == true
        }
    }

    @SubscribeEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (!isEnabled() || eventShops.isEmpty() || currentProgress == null || event.slot != CUSTOM_STACK_LOCATION) return
        if (!eventShops.any { it.shopName.equals(event.inventory.name, ignoreCase = true) }) return
        infoItemStack.let { event.replace(it) }
    }

    @SubscribeEvent
    fun onNeuRepoReload(event: NeuRepositoryReloadEvent) {
        val repoTokenShops = event.readConstant<NeuMiscJson>("carnivalshops").carnivalTokenShops
        eventShops = repoTokenShops.map { (key, value) ->
            EventShop(key.replace("_", " "), value.values.toMutableList())
        }.toMutableList()
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        currentProgress = null
        currentEventType = ""
        tokensOwned = 0
        tokensNeeded = 0
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        processInventoryEvent(event)
    }

    @SubscribeEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        processInventoryEvent(event)
    }

    private fun regenerateItemStack() {
        infoItemStack = currentProgress?.let { progress ->
            createItemStack(
                "NAME_TAG".asInternalName().getItemStack().item,
                "§bRemaining $currentEventType Token Upgrades",
                *buildList {
                    val remaining = progress.remainingUpgrades.filter { it.remainingCosts.isNotEmpty() }
                    if (remaining.isEmpty()) add("§a§lAll upgrades purchased!")
                    else {
                        add("")
                        remaining.forEach {
                            add(
                                "  §a${it.upgradeName} §b${it.currentLevel} §7-> §b${it.maxLevel}§7: §8${
                                    it.remainingCosts.sum().addSeparators()
                                }",
                            )
                        }
                        add("")

                        val upgradeTotal = remaining.sumOf { it.remainingCosts.sum() }
                        add("§7Sum Tokens Needed: §8${upgradeTotal.addSeparators()}")
                        tokensNeeded = upgradeTotal - tokensOwned
                        if (tokensOwned > 0) add("§7Tokens Owned: §8${tokensOwned.addSeparators()}")
                        if (tokensNeeded > 0) add("§7Additional Tokens Needed: §8${tokensNeeded.addSeparators()}")
                        else addAll(listOf("", "§e§oYou have enough tokens!"))
                    }

                    if (progress.nonRepoUpgrades.any()) {
                        add("")
                        add("§cFound upgrades not in repo§c§l:")
                        progress.nonRepoUpgrades.forEach { add("  §4${it.key}") }
                    }
                }.toTypedArray(),
            )
        }
    }

    private fun processInventoryEvent(event: InventoryOpenEvent) {
        if (!isEnabled() || eventShops.isEmpty()) return
        val matchingShop = eventShops.find { it.shopName.equals(event.inventoryName, ignoreCase = true) } ?: return
        currentEventType = matchingShop.shopName
        processEventShopUpgrades(event.inventoryItems)
        processTokenShopFooter(event)
        regenerateItemStack()
    }

    private fun processTokenShopFooter(event: InventoryOpenEvent) {
        val tokenFooterStack = event.inventoryItems[32]
        if (tokenFooterStack === null || tokenFooterStack.displayName != "§eCarnival Tokens") {
            ErrorManager.logErrorWithData(
                NoSuchElementException(""),
                "Could not read current Essence Count from inventory",
                extraData = listOf(
                    "inventoryName" to event.inventoryName,
                    "tokenFooterStack" to tokenFooterStack?.displayName.orEmpty(),
                    "populatedInventorySize" to event.inventoryItems.filter { it.value.hasDisplayName() }.size,
                    "eventType" to event.javaClass.simpleName,
                ).toTypedArray(),
            )
            return
        }
        currentTokenCountPattern.firstMatcher(tokenFooterStack.getLore()) {
            tokensOwned = groupOrNull("tokens")?.formatInt() ?: 0
        }
    }

    private fun processEventShopUpgrades(inventoryItems: Map<Int, ItemStack>) {
        /**
         * All upgrades will appear in slots 11 -> 15
         *
         * Filter out items outside of these bounds
         */
        val upgradeStacks = inventoryItems.filter { it.key in 11..15 && it.value.item != null }
        val purchasedUpgrades: MutableMap<String, Int> = buildMap {
            upgradeStacks.forEach {
                // Right now Carnival and Essence Upgrade patterns are 'in-sync'
                // This may change in the future, and this would then need its own pattern
                essenceUpgradePattern.matchMatcher(it.value.displayName) {
                    val upgradeName = groupOrNull("upgrade") ?: return
                    val nextUpgradeRoman = groupOrNull("tier") ?: return
                    val nextUpgrade = nextUpgradeRoman.romanToDecimal()
                    val isMaxed = it.value.getLore().any { loreLine -> maxedUpgradeLorePattern.matches(loreLine) }
                    put(upgradeName, if (isMaxed) nextUpgrade else nextUpgrade - 1)
                }
            }
        }.toMutableMap()
        currentProgress = EventShopProgress(currentEventType, purchasedUpgrades)
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && SkyHanniMod.feature.event.carnival.tokenShopHelper
}
