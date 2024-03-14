package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.utils.InventoryUtils.getInventoryName
import at.hannibal2.skyhanni.utils.InventoryUtils.getUpperItems
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class TabWidgetSettings {

    private val mainPageWidgetPattern = "^§7Currently:.*".toRegex()

    private val subPageWidgetPattern = "^§eClick to .*".toRegex(RegexOption.IGNORE_CASE)

    private val shownSettingPattern = "Shown .* Setting.*".toRegex(RegexOption.IGNORE_CASE)

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (event.gui !is GuiChest) return
        if (!SkyHanniMod.feature.misc.highlightWidgets) return

        val chest = event.gui.inventorySlots as ContainerChest
        val inventoryName = chest.getInventoryName()
        if (inventoryName.startsWith("Widgets ")) {
            val items = chest.getUpperItems().filter { it.value.getLore().any { mainPageWidgetPattern.matches(it) } }
            for ((slot, stack) in items) {
                if (stack.getLore().any() { it.contains("ENABLED") }) {
                    slot highlight LorenzColor.GREEN
                } else {
                    slot highlight LorenzColor.RED
                }
            }
        }

        if (inventoryName.endsWith("Widget Settings") || shownSettingPattern.matches(inventoryName)) {

            val items = chest.getUpperItems().filter {
                val loreLastLine = it.value.getLore().lastOrNull()
                subPageWidgetPattern.matches(loreLastLine ?: "")
            }

            for ((slot, stack) in items) {
                val loreLastLine = stack.getLore().lastOrNull()

                if (subPageWidgetPattern.matches(loreLastLine ?: "")) {
                    if (stack.getLore().any() { it.contains("disable!") }) {
                        slot highlight LorenzColor.GREEN
                    } else {
                        slot highlight LorenzColor.RED
                    }
                }
            }
        }

    }
}
