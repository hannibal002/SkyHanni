package at.hannibal2.skyhanni.features.garden.composter

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.InventoryUtils.getUpperItems
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.world.inventory.ChestMenu

@SkyHanniModule
object GardenComposterInventoryFeatures {

    private val config get() = GardenApi.config.composters

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onToolTip(event: ToolTipTextEvent) {
        if (!config.upgradePrice) return

        if (InventoryUtils.openInventoryName() != "Composter Upgrades") return

        var next = false
        val list = event.toolTip
        var i = -1
        var indexFullCost = 0
        var fullPrice = 0.0
        var amountItems = 0
        for (line in event.toolTip) {
            i++
            if (line.string == "Upgrade Cost:") {
                next = true
                indexFullCost = i
                continue
            }

            if (next) {
                if (line.string.endsWith(" Copper")) continue
                if (line.string == "") break
                val (itemName, amount) = ItemUtils.readItemAmount(line.formattedTextCompatLessResets().removePrefix("§5")) ?: run {
                    ErrorManager.logErrorStateWithData(
                        "Error reading item line",
                        "could not read item line",
                        "line" to line,
                    )
                    continue
                }
                val internalName = NeuInternalName.fromItemName(itemName)
                val lowestBin = internalName.getPrice()
                val price = lowestBin * amount
                fullPrice += price
                val format = price.shortFormat()
                list[i] = list[i].append(" §7(§6$format§7)")
                amountItems++
            }
        }

        if (amountItems > 1) {
            val format = fullPrice.shortFormat()
            list[indexFullCost] = list[indexFullCost].append(" §7(§6$format§7)")
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!config.highlightUpgrade) return

        if (InventoryUtils.openInventoryName() == "Composter Upgrades") {
            if (event.gui !is ContainerScreen) return
            val chest = event.container as ChestMenu

            for ((slot, stack) in chest.getUpperItems()) {
                if (stack.getLore().any { it == "§eClick to upgrade!" }) {
                    slot.highlight(LorenzColor.GOLD)
                }
            }
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.composterUpgradePrice", "garden.composters.upgradePrice")
        event.move(3, "garden.composterHighLightUpgrade", "garden.composters.highlightUpgrade")
    }
}
