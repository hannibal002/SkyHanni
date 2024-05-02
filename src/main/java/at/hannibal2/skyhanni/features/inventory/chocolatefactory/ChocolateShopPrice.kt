package at.hannibal2.skyhanni.features.inventory.chocolatefactory

import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.utils.DisplayTableEntry
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems.getPriceOrNull
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.million
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ChocolateShopPrice {
    private val config get() = ChocolateFactoryAPI.config.chocolateShopPrice

    private var display = emptyList<Renderable>()

    private val menuNamePattern by ChocolateFactoryAPI.patternGroup.pattern(
        "shop.title",
        "Chocolate Shop"
    )

    var inInventory = false
    var inventoryItems = emptyMap<Int, ItemStack>()

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (inInventory) {
            update()
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        if (!menuNamePattern.matches(event.inventoryName)) return

        inInventory = true
        inventoryItems = event.inventoryItems
        update()
    }

    private fun update() {
        val multiplier = 1.million
        // TODO merge core with SkyMartCopperPrice into a utils
        val table = mutableListOf<DisplayTableEntry>()
        val inventoryItems = inventoryItems
        for ((slot, item) in inventoryItems) {
            val lore = item.getLore()
            val chocolate = ChocolateFactoryAPI.getChocolateBuyCost(lore) ?: continue
            val internalName = item.getInternalName()
            val itemPrice = internalName.getPriceOrNull() ?: continue

            val factor = (itemPrice / chocolate) * multiplier
            val perFormat = NumberUtil.format(factor)

            val itemName = item.itemName
            val hover = buildList {
                add(itemName)

                add("")
                add("§7Item price: §6${NumberUtil.format(itemPrice)} ")
                add("§7Chocolate amount: §c${NumberUtil.format(chocolate)} ")

                add("")
                add("§7Profit per million chocolate: §6${perFormat} ")

                add("")
                val formattedTimeUntilGoal = ChocolateAmount.CURRENT.formattedTimeUntilGoal(chocolate)
                add("§7Time until affordable: §6$formattedTimeUntilGoal ")

                add("")
                val chocolatePerHour = ChocolateFactoryAPI.chocolatePerSecond * 60 * 60
                val profitPerHour = chocolatePerHour * (factor / 1000000)
                add("§7Money per hour: §6${NumberUtil.format(profitPerHour)} ")

            }
            table.add(
                DisplayTableEntry(
                    "$itemName§f:",
                    "§6§l$perFormat",
                    factor,
                    internalName,
                    hover,
                    highlightsOnHoverSlots = listOf(slot)
                )
            )
        }

        val newList = mutableListOf<Renderable>()
        newList.add(Renderable.string("§e§lCoins per million chocolate§f:"))
        // TODO update this value every second
        // TODO add time until can afford
        newList.add(Renderable.string("§eChocolate available: §6${ChocolateAmount.CURRENT.formatted}"))
        newList.add(LorenzUtils.fillTable(table, padding = 5, itemScale = config.itemScale))
        display = newList
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (inInventory) {
            config.position.renderRenderables(
                display,
                extraSpace = 5,
                posLabel = "Chocolate Shop Price"
            )
        }
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
