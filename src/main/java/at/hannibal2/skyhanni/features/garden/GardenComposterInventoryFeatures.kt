package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenComposterInventoryFeatures {
    val config get() = SkyHanniMod.feature.garden

    @SubscribeEvent
    fun onTooltip(event: ItemTooltipEvent) {
        if (!GardenAPI.inGarden()) return
        if (!config.composterUpgradePrice) return

        if (InventoryUtils.openInventoryName() != "Composter Upgrades") return

        var next = false
        val list = event.toolTip
        var i = -1
        var indexFullCost = 0
        var fullPrice = 0.0
        var amountItems = 0
        for (originalLine in list) {
            i++
            val line = originalLine.substring(4)
            if (line == "§7Upgrade Cost:") {
                next = true
                indexFullCost = i
                continue
            }

            if (next) {
                if (line.endsWith(" Copper")) continue
                if (line == "") break
                val (itemName, amount) = ItemUtils.readItemAmount(line)
                if (itemName == null) {
                    LorenzUtils.error("§c[SkyHanni] Could not read item '$line'")
                    continue
                }
                val lowestBin = NEUItems.getPrice(NEUItems.getInternalName(itemName))
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
        if (!config.composterHighLightUpgrade) return

        if (InventoryUtils.openInventoryName() == "Composter Upgrades") {
            if (event.gui !is GuiChest) return
            val guiChest = event.gui
            val chest = guiChest.inventorySlots as ContainerChest

            for (slot in chest.inventorySlots) {
                if (slot == null) continue
                if (slot.slotNumber != slot.slotIndex) continue
                val stack = slot.stack ?: continue

                if (stack.getLore().any { it == "§eClick to upgrade!" }) {
                    slot highlight LorenzColor.GOLD
                }
            }
        }
    }
}