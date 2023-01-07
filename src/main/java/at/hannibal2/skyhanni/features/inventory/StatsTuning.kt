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
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class StatsTuning {

    private val patternStatPoints = Pattern.compile("§7Stat has: §e(\\d+) point(s)?")

    @SubscribeEvent
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        val screen = Minecraft.getMinecraft().currentScreen
        if (screen !is GuiChest) return
        val chest = screen.inventorySlots as ContainerChest
        val chestName = chest.lowerChestInventory.displayName.unformattedText.trim()

        val stack = event.stack

        if (SkyHanniMod.feature.inventory.statsTuningTemplateStats) {
            if (chestName == "Stats Tuning") {
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
        }
        if (SkyHanniMod.feature.inventory.statsTuningSelectedStats) {
            if (chestName == "Accessory Bag Thaumaturgy") {
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
        }
        if (SkyHanniMod.feature.inventory.statsTuningPoints) {
            if (chestName == "Stats Tuning") {
                for (line in stack.getLore()) {
                    val matcher = patternStatPoints.matcher(line)
                    if (matcher.matches()) {
                        val points = matcher.group(1)
                        event.stackTip = points
                    }
                }
            }
        }

    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onDrawSelectedTemplate(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!LorenzUtils.inSkyBlock) return

        if (event.gui !is GuiChest) return
        val guiChest = event.gui
        val chest = guiChest.inventorySlots as ContainerChest
        val chestName = chest.lowerChestInventory.displayName.unformattedText.trim()

        if (SkyHanniMod.feature.inventory.statsTuningSelectedTemplate) {
            if (chestName == "Stats Tuning") {
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
}