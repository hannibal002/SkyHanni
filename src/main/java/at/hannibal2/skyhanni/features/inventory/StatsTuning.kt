package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class StatsTuning {
    private val patternStatPoints = "§7Stat has: §e(?<amount>\\d+) points?".toPattern()

    @SubscribeEvent
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        val inventoryName = event.inventoryName

        val stack = event.stack

        if (SkyHanniMod.feature.inventory.statsTuningTemplateStats && inventoryName == "Stats Tuning") {
            val name = stack.name ?: return
            if (name == "§aLoad") {
                var grab = false
                val list = mutableListOf<String>()
                for (line in stack.getLore()) {
                    if (line == "§7You are loading:") {
                        grab = true
                        continue
                    }
                    if (grab) {
                        if (line == "") {
                            grab = false
                            continue
                        }
                        val text = line.split(":")[0]
                        list.add(text)
                    }
                }
                if (list.isNotEmpty()) {
                    event.stackTip = list.joinToString(" + ")
                    event.offsetX = 20
                    event.offsetY = -5
                    event.alignLeft = false
                    return
                }
            }
        }
        if (SkyHanniMod.feature.inventory.statsTuningSelectedStats && inventoryName == "Accessory Bag Thaumaturgy") {
            val name = stack.name ?: return
            if (name == "§aStats Tuning") {
                var grab = false
                val list = mutableListOf<String>()
                for (line in stack.getLore()) {
                    if (line == "§7Your tuning:") {
                        grab = true
                        continue
                    }
                    if (grab) {
                        if (line == "") {
                            grab = false
                            continue
                        }
                        val text = line.split(":")[0].split(" ")[0] + "§7"
                        list.add(text)
                    }
                }
                if (list.isNotEmpty()) {
                    event.stackTip = list.joinToString(" + ")
                    event.offsetX = 3
                    event.offsetY = -5
                    event.alignLeft = false
                    return
                }
            }
        }
        if (SkyHanniMod.feature.inventory.statsTuningPoints && inventoryName == "Stats Tuning") {
            for (line in stack.getLore()) {
                patternStatPoints.matchMatcher(line) {
                    val points = group("amount")
                    event.stackTip = points
                }
            }
        }

    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onDrawSelectedTemplate(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val chestName = InventoryUtils.openInventoryName()
        if (SkyHanniMod.feature.inventory.statsTuningSelectedTemplate && chestName == "Stats Tuning") {
            for (slot in InventoryUtils.getItemsInOpenChest()) {
                val stack = slot.stack
                val lore = stack.getLore()

                if (lore.any { it == "§aCurrently selected!" }) {
                    slot highlight LorenzColor.GREEN
                }
            }
        }
    }
}