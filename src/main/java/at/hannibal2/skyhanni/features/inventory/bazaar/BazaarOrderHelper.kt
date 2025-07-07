package at.hannibal2.skyhanni.features.inventory.bazaar

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi.getBazaarData
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi.getBazaarDataOrError
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils.getUpperItems
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraft.item.ItemStack

@SkyHanniModule
object BazaarOrderHelper {
    private val patternGroup = RepoPattern.group("bazaar.orderhelper")
    private val config get() = SkyHanniMod.feature.inventory.bazaar

    private var highlightedSlots = mapOf<Int, LorenzColor>()

    /**
     * REGEX-TEST: §a§lBUY §fWheat
     */
    private val bazaarItemNamePattern by patternGroup.pattern(
        "itemname",
        "§.§l(?<type>BUY|SELL) (?<name>.*)",
    )

    /**
     * REGEX-TEST: §7Filled: §a200§7/200 §a§l100%!
     */
    private val filledPattern by patternGroup.pattern(
        "filled",
        "§7Filled: §[a6].*§7/.* §a§l100%!",
    )

    /**
     * REGEX-TEST: §7Price per unit: §63.1 coins
     */
    private val pricePattern by patternGroup.pattern(
        "price",
        "§7Price per unit: §6(?<number>.*) coins",
    )

    private val inventory = InventoryDetector(
        openInventory = { highlightedSlots = load(it.inventoryItems) },
        checkInventoryName = { name -> BazaarApi.isBazaarOrderInventory(name) && config.orderHelper },
    )


    private fun load(inventoryItems: Map<Int, ItemStack>): Map<Int, LorenzColor> {
        val slots = mutableMapOf<Int, LorenzColor>()
        val errorItems = mutableSetOf<NeuInternalName>()
        for ((slot, stack) in inventoryItems) {
            bazaarItemNamePattern.matchMatcher(stack.displayName) {
                val buyOrSell = group("type").let { (it == "BUY") to (it == "SELL") }
                if (buyOrSell.let { !it.first && !it.second }) return@matchMatcher

                val internalName = NeuInternalName.fromItemName(group("name"))
                internalName.getBazaarData()?.let {
                    highlightItem(slot, stack, buyOrSell, it, slots)
                } ?: run {
                    errorItems.add(internalName)
                }
            }
        }
        errorItems.firstOrNull()?.getBazaarDataOrError()

        return slots
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!inventory.isInside()) return
        if (event.gui !is GuiChest) return
        val chest = event.container as ContainerChest
        for ((slot, _) in chest.getUpperItems()) {
            highlightedSlots[slot.slotNumber]?.let {
                slot.highlight(it)
            }
        }
    }

    private fun highlightItem(
        slot: Int,
        stack: ItemStack,
        buyOrSell: Pair<Boolean, Boolean>,
        data: BazaarData,
        map: MutableMap<Int, LorenzColor>,
    ) {
        for (line in stack.getLore()) {
            filledPattern.matchMatcher(line) {
                map[slot] = LorenzColor.GREEN
                return
            }

            pricePattern.matchMatcher(line) {
                val price = group("number").formatDouble()
                if (buyOrSell.first && price < data.instantSellPrice || buyOrSell.second && price > data.instantBuyPrice) {
                    map[slot] = LorenzColor.GOLD
                    return
                }
            }
        }
    }
}
