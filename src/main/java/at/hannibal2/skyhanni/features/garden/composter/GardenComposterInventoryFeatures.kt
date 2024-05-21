package at.hannibal2.skyhanni.features.garden.composter

import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.InventoryUtils.getUpperItems
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.StringUtils.anyFound
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenComposterInventoryFeatures {

    private val config get() = GardenAPI.config.composters

    private val patternGroup = RepoPattern.group("gardencomposterinventoryfeatures")
    private val costPattern by patternGroup.pattern(
        "cost",
        "§7Upgrade Cost:"
    )
    private val copperPattern by patternGroup.pattern(
        "copper",
        " Copper$"
    )
    private val clickUpgradePattern by patternGroup.pattern(
        "clickupgrade",
        "§eClick to upgrade!"
    )

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!GardenAPI.inGarden()) return
        if (!config.upgradePrice) return

        if (InventoryUtils.openInventoryName() != "Composter Upgrades") return

        var next = false
        val list = event.toolTip
        var i = -1
        var indexFullCost = 0
        var fullPrice = 0.0
        var amountItems = 0
        for (line in event.toolTipRemovedPrefix()) {
            i++
            if (costPattern.matches(line)) {
                next = true
                indexFullCost = i
                continue
            }

            if (next) {
                if (copperPattern.matches(line)) continue
                if (line == "") break
                val (itemName, amount) = ItemUtils.readItemAmount(line) ?: run {
                    ErrorManager.logErrorStateWithData(
                        "Error reading item line",
                        "could not read item line",
                        "line" to line
                    )
                    continue
                }
                val internalName = NEUInternalName.fromItemNameOrNull(itemName) ?: run {
                    ErrorManager.logErrorStateWithData(
                        "Error reading internal name for item: $itemName",
                        "could not find internal name for",
                        "itemName" to itemName
                    )
                    continue
                }
                val lowestBin = internalName.getPrice()
                val price = lowestBin * amount
                fullPrice += price
                val format = NumberUtil.format(price)
                list[i] = list[i] + " §7(§6$format§7)"
                amountItems++
            }
        }

        if (amountItems > 1) {
            val format = NumberUtil.format(fullPrice)
            list[indexFullCost] = list[indexFullCost] + " §7(§6$format§7)"
        }
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.highlightUpgrade) return

        if (InventoryUtils.openInventoryName() == "Composter Upgrades") {
            if (event.gui !is GuiChest) return
            val guiChest = event.gui
            val chest = guiChest.inventorySlots as ContainerChest

            for ((slot, stack) in chest.getUpperItems()) {
                if (clickUpgradePattern.anyFound(stack.getLore())) {
                    slot highlight LorenzColor.GOLD
                }
            }
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.composterUpgradePrice", "garden.composters.upgradePrice")
        event.move(3, "garden.composterHighLightUpgrade", "garden.composters.highlightUpgrade")
    }
}
