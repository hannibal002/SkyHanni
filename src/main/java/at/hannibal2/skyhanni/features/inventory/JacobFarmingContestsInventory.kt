package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.LateInventoryOpenEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class JacobFarmingContestsInventory {

    private val duplicateSlots = mutableListOf<Int>()
    private val realTime = mutableMapOf<Int, String>()

    private val formatDay = SimpleDateFormat("dd MMMM yyyy", Locale.US)
    private val formatTime = SimpleDateFormat("HH:mm", Locale.US)
    private val pattern = Pattern.compile("§a(.*) (.*)(?:rd|st|nd|th), Year (.*)")
    private val config get() = SkyHanniMod.feature.inventory

    // Render the contests a tick delayed to feel smoother
    private var hideEverything = true

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        duplicateSlots.clear()
        realTime.clear()
        hideEverything = true
    }

    @SubscribeEvent
    fun onLateInventoryOpen(event: LateInventoryOpenEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (event.inventoryName != "Your Contests") return

        duplicateSlots.clear()
        realTime.clear()

        val foundEvents = mutableListOf<String>()
        for ((slot, item) in event.inventoryItems) {
            if (!item.getLore().any { it.startsWith("§7Your score: §e") }) continue

            val name = item.name!!

            if (foundEvents.contains(name)) {
                if (config.jacobFarmingContestHideDuplicates) {
                    duplicateSlots.add(slot)
                }
            } else {
                foundEvents.add(name)
            }
            if (config.jacobFarmingContestRealTime) {
                readRealTime(name, slot)
            }
        }
        hideEverything = false
    }

    private fun readRealTime(name: String, slot: Int) {
        val matcher = pattern.matcher(name)
        if (!matcher.matches()) return

        val month = matcher.group(1)
        val day = matcher.group(2).toInt()
        val year = matcher.group(3).toInt()

        val monthNr = LorenzUtils.getSBMonthByName(month)
        val time = SkyBlockTime(year, monthNr, day)
        val toMillis = time.toMillis()
        val dayFormat = formatDay.format(toMillis)
        val startTimeFormat = formatTime.format(toMillis)
        val endTimeFormat = formatTime.format(toMillis + 1000 * 60 * 20)
        realTime[slot] = "$dayFormat $startTimeFormat-$endTimeFormat"
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!InventoryUtils.openInventoryName().contains("Your Contests")) return
        if (!config.jacobFarmingContestHighlightRewards) return

        // hide green border for a tick
        if (config.jacobFarmingContestHideDuplicates && hideEverything) return

        if (event.gui !is GuiChest) return
        val guiChest = event.gui
        val chest = guiChest.inventorySlots as ContainerChest

        for (slot in chest.inventorySlots) {
            if (slot == null) continue
            if (slot.slotNumber != slot.slotIndex) continue
            if (duplicateSlots.contains(slot.slotNumber)) continue
            val stack = slot.stack ?: continue
            if (stack.getLore().any { it == "§eClick to claim reward!" }) {
                slot highlight LorenzColor.GREEN
            }
        }
    }

    @SubscribeEvent
    fun onDrawSlot(event: GuiContainerEvent.DrawSlotEvent.GuiContainerDrawSlotPre) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.jacobFarmingContestHideDuplicates) return
        if (!InventoryUtils.openInventoryName().contains("Your Contests")) return

        if (hideEverything) {
            val slot = event.slot
            val number = slot.slotNumber
            if (number in 10..43) {
                event.isCanceled = true
                return
            }

        }

        val slot = event.slot.slotNumber
        if (!duplicateSlots.contains(slot)) return
        event.isCanceled = true
    }

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!InventoryUtils.openInventoryName().contains("Your Contests")) return

        val slot = event.slot.slotNumber

        if (config.jacobFarmingContestHideDuplicates) {
            if (duplicateSlots.contains(slot)) {
                event.toolTip.clear()
                event.toolTip.add("§7Duplicate contest!")
                return
            }
        }

        if (config.jacobFarmingContestRealTime) {
            realTime[slot]?.let {
                val toolTip = event.toolTip
                if (toolTip.size > 1) {
                    toolTip.add(1, it)
                }
            }
        }
    }
}