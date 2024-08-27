package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.jsonobjects.repo.EssenceShopUpgrade
import at.hannibal2.skyhanni.data.jsonobjects.repo.EssenceShopsJson
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import com.google.gson.Gson
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object EssenceShopHelper {

    private var essenceShops = mutableListOf<EssenceShop>()
    private var currentProgress: EssenceShopProgress? = null
    private var ownedEssence: Int = 0

    /**
     * REGEX-TEST: Gold Essence Shop
     * REGEX-TEST: Wither Essence Shop
     */
    private val essenceShopPattern by patternGroup.pattern(
        "essence.shop",
        "(?<essence>.*) Essence Shop",
    )

    /**
     * REGEX-TEST: §7Your Undead Essence: §d12,664
     * REGEX-TEST: §7Your Wither Essence: §d2,275
     */
    private val currentEssenceCountPattern by patternGroup.pattern(
        "essence.current",
        ".*§7Your (?<essence>.*) Essence: §.(?<count>[\\d,]*)"
    )


    /**
     * REGEX-TEST: §aHigh Roller I
     * REGEX-TEST: §aForbidden Speed III
     * REGEX-TEST: §aReturn to Sender X
     */
    private val essenceUpgradePattern by patternGroup.pattern(
        "essence.upgrade",
        "§.(?<upgrade>.*) (?<tier>[IVXLCDM].*)"
    )

    data class EssenceShop(val shopName: String, val upgrades: List<EssenceShopUpgrade>) {
        val totalCost: Int = upgrades.sumOf { it.costs.sum() }
    }

    data class EssenceShopProgress(val essenceName: String, val purchasedUpgrades: Map<String, Int>) {
        private val essenceShop = essenceShops.find { it.shopName.equals(essenceName, ignoreCase = true) }
        val remainingUpgrades: MutableMap<String, MutableList<Int>> = essenceShop?.upgrades?.associate {
            it.name to buildList {
                val purchasedAmount = purchasedUpgrades[it.name]
                if (purchasedAmount == null) addAll(it.costs ?: emptyList())
                else addAll(it.costs.drop(purchasedAmount) ?: emptyList())
            }.toMutableList()
        }?.toMutableMap() ?: mutableMapOf()
        val nonRepoUpgrades = purchasedUpgrades.any { purchasedUpgrade ->
            essenceShop?.upgrades?.none { it.name.equals(purchasedUpgrade.key, ignoreCase = true) } == true
        }
    }

    @SubscribeEvent
    fun onNeuRepoReload(event: NeuRepositoryReloadEvent) {
        val data = event.readConstant<EssenceShopsJson>("esenceshops", Gson())
        essenceShops = data.shops.map { (key, value) ->
            EssenceShop(key, value.upgrades.values.toMutableList() )
        }.toMutableList()
    }

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!enabled() || !inEssenceShop() || essenceShops.isEmpty() || currentProgress == null || event.slot.slotNumber != 4) return
        ChatUtils.chat("In tooltip event")
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        currentProgress = null
    }

    @SubscribeEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        processInventoryEvent(event)
    }

    @SubscribeEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        processInventoryEvent(event)
    }

    private fun processInventoryEvent(event: InventoryOpenEvent) {
        if (!enabled() || !inEssenceShop() || essenceShops.isEmpty()) return
        essenceShopPattern.matchMatcher(event.inventoryName) {
            val essenceName = groupOrNull("essence")?.let { "${it.uppercase()}_ESSENCE" } ?: return
            essenceShops.find { it.shopName == essenceName } ?: return
            processEssenceShopUpgrades(essenceName, event.inventoryItems)
            processEssenceShopHeader(essenceName, event)
        }
    }

    private fun processEssenceShopHeader(essenceName: String, event: InventoryOpenEvent) {
        val essenceHeaderStack = event.inventoryItems[4]
        if (essenceHeaderStack == null || !essenceShopPattern.matches(essenceHeaderStack.itemName))
            ErrorManager.skyHanniError("Could not read current Essence Count from inventory §c§l${event.inventoryName}")

        currentEssenceCountPattern.firstMatcher(essenceHeaderStack.getLore()) {
            ownedEssence = groupOrNull("count")?.toInt() ?: 0
        }
    }

    private fun processEssenceShopUpgrades(essenceName: String, inventoryItems: Map<Int, ItemStack>) {
        /**
         * Essence Upgrade Bounds
         * Undead  -> 10 to 20
         * Wither  -> 10 to 16
         * Dragon  -> 19 to 33
         * Spider  -> 19 to 25
         * Crimson -> 20 to 33
         * Ice     -> 21 to 32
         * Gold    -> 19 to 25
         * Diamond -> 19 to 25
         *
         * Filter out items that fall outside the bounds of 10 - 33
         */
        val upgradeStacks = inventoryItems.filter { it.key in 10..33 && it.value.item != null}
        val purchasedUpgrades: MutableMap<String, Int> = buildMap {
            upgradeStacks.forEach {
                essenceUpgradePattern.matchMatcher(it.value.itemName) {
                    val upgradeName = groupOrNull("upgrade") ?: return
                    val nextUpgradeRoman = groupOrNull("tier") ?: return
                    val nextUpgrade = nextUpgradeRoman.romanToDecimal()
                    val isMaxed = it.value.getLore().any { loreLine -> Regex(".*§a§lUNLOCKED").containsMatchIn(loreLine) }
                    put(upgradeName, if (isMaxed) nextUpgrade else nextUpgrade - 1 )
                }
            }
        }.toMutableMap()
        currentProgress = EssenceShopProgress(essenceName, purchasedUpgrades)
    }

    private fun inEssenceShop() = essenceShopPattern.matches(InventoryUtils.openInventoryName())
    private fun enabled() = LorenzUtils.inSkyBlock && SkyHanniMod.feature.inventory.essenceShopHelper
}
