package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.utils.InventoryUtils.getInventoryName
import at.hannibal2.skyhanni.utils.InventoryUtils.getUpperItems
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraft.inventory.Slot
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class TabWidgetSettings {

//     private val mainPageWidgetPattern = "^§7Currently:.*".toRegex()
//
//     private val subPageWidgetPattern = "^§eClick to .*".toRegex(RegexOption.IGNORE_CASE)
//
//     private val shownSettingPattern = "Shown .* Setting.* |.*Widget Settings".toRegex(RegexOption.IGNORE_CASE)

    private val repoGroup = RepoPattern.group("tab.widget.setting")
    private val mainPageSettingPattern by repoGroup.pattern("gui","^WIDGETS.*")
    private val mainPageWidgetPattern by repoGroup.pattern("main","^§7Currently:.*")
    private val subPageWidgetPattern by repoGroup.pattern("sub","^§eClick to .*")
    private val shownSettingPattern by repoGroup.pattern("show","Shown .* Setting.* |.*Widget Settings")

    var inInventory = false;
    private var highlights = mutableMapOf<Int, LorenzColor>()

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.misc.highlightWidgets) return
        highlights.clear()

//         val chest = event.gui.inventorySlots as ContainerChest
        val inventoryName = event.inventoryName
        println(mainPageSettingPattern.matches(inventoryName.uppercase()))
        if (mainPageSettingPattern.matches(inventoryName.uppercase())) {
            val items = event.inventoryItems.filter { it.value.getLore().any { mainPageWidgetPattern.matches(it) } }
            for ((slot, stack) in items) {
                if (stack.getLore().any() { it.contains("ENABLED") }) {
                    highlights[slot] = LorenzColor.GREEN
                } else {
                    highlights[slot] = LorenzColor.RED
                }
            }
        }

        if (shownSettingPattern.matches(inventoryName)) {

            val items = event.inventoryItems.filter {
                val loreLastLine = it.value.getLore().lastOrNull()
                subPageWidgetPattern.matches(loreLastLine ?: "")
            }

            for ((slot, stack) in items) {
                val loreLastLine = stack.getLore().lastOrNull()

                if (subPageWidgetPattern.matches(loreLastLine ?: "")) {
                    if (stack.getLore().any() { it.contains("disable!") }) {
                        highlights[slot] = LorenzColor.GREEN
                    } else {
                        highlights[slot] = LorenzColor.RED
                    }
                }
            }
        }


    }
    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
        highlights.clear()
    }

    @SubscribeEvent
    fun onInventoryClose(event: GuiContainerEvent.CloseWindowEvent) {
        inInventory = false
        highlights.clear()
    }


    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.misc.highlightWidgets) return

        event.gui.inventorySlots.inventorySlots
            .associateWith { highlights[it.slotNumber] }
            .forEach { (slot, color) ->
                color?.let { slot.highlight(it) }
            }
    }
}
