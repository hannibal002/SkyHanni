package at.hannibal2.skyhanni.features.garden.contest

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderItemEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.drawSlotText
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.text.SimpleDateFormat
import java.util.Locale

class JacobFarmingContestsInventory {
    private val realTime = mutableMapOf<Int, String>()

    private val formatDay = SimpleDateFormat("dd MMMM yyyy", Locale.US)
    private val formatTime = SimpleDateFormat("HH:mm", Locale.US)
    private val config get() = SkyHanniMod.feature.inventory.jacobFarmingContests

    // Render the contests a tick delayed to feel smoother
    private var hideEverything = true
    private val contestEarnedPattern = "§7You earned a §(?<medalColour>.*)§l.* §7medal!".toPattern()

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        realTime.clear()
        hideEverything = true
    }

    @SubscribeEvent
    fun onLateInventoryOpen(event: InventoryUpdatedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (event.inventoryName != "Your Contests") return

        realTime.clear()

        val foundEvents = mutableListOf<String>()
        for ((slot, item) in event.inventoryItems) {
            if (!item.getLore().any { it.startsWith("§7Your score: §e") }) continue

            val name = item.name!!

            if (foundEvents.contains(name)) {
            } else {
                foundEvents.add(name)
            }
            val time = FarmingContestAPI.getSbTimeFor(name) ?: continue
            FarmingContestAPI.addContest(time, item)
            if (config.realTime) {
                readRealTime(time, slot)
            }
        }
        hideEverything = false
    }

    private fun readRealTime(time: Long, slot: Int) {
        val dayFormat = formatDay.format(time)
        val startTimeFormat = formatTime.format(time)
        val endTimeFormat = formatTime.format(time + 1000 * 60 * 20)
        realTime[slot] = "$dayFormat $startTimeFormat-$endTimeFormat"
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!InventoryUtils.openInventoryName().contains("Your Contests")) return
        if (!config.highlightRewards) return

        // hide green border for a tick
        if (hideEverything) return

        if (event.gui !is GuiChest) return
        val guiChest = event.gui
        val chest = guiChest.inventorySlots as ContainerChest

        for (slot in chest.inventorySlots) {
            if (slot == null) continue
            if (slot.slotNumber != slot.slotIndex) continue
            val stack = slot.stack ?: continue
            if (stack.getLore().any { it == "§eClick to claim reward!" }) {
                slot highlight LorenzColor.GREEN
            }
        }
    }

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!InventoryUtils.openInventoryName().contains("Your Contests")) return

        val slot = event.slot.slotNumber
        if (config.realTime) {
            realTime[slot]?.let {
                val toolTip = event.toolTip
                if (toolTip.size > 1) {
                    toolTip.add(1, it)
                }
            }
        }
    }

    @SubscribeEvent
    fun onRenderItemOverlayPost(event: GuiRenderItemEvent.RenderOverlayEvent.GuiRenderItemPost) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.medalIcon) return
        if (!InventoryUtils.openInventoryName().contains("Your Contests")) return

        val stack = event.stack ?: return
        var finneganContest = false

        for (line in stack.getLore()) {
            if (line.contains("Contest boosted by Finnegan!")) finneganContest = true

            val matcher = contestEarnedPattern.matcher(line)
            if (matcher.matches()) {
                val medalEarned = ContestBracket.entries.find { it.color == matcher.group("medalColour") } ?: return

                var stackTip = "§${medalEarned.color}✦"
                var x = event.x + 9
                var y = event.y + 1
                var scale = .7f

                if (finneganContest && config.finneganIcon) {
                    stackTip = "§${medalEarned.color}▲"
                    x = event.x + 5
                    y = event.y - 2
                    scale = 1.3f
                }

                event.drawSlotText(x, y, stackTip, scale)
            }
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(
            3,
            "inventory.jacobFarmingContestHighlightRewards",
            "inventory.jacobFarmingContests.highlightRewards"
        )
        event.move(3, "inventory.jacobFarmingContestHideDuplicates", "inventory.jacobFarmingContests.hideDuplicates")
        event.move(3, "inventory.jacobFarmingContestRealTime", "inventory.jacobFarmingContests.realTime")
        event.move(3, "inventory.jacobFarmingContestFinneganIcon", "inventory.jacobFarmingContests.finneganIcon")
        event.move(3, "inventory.jacobFarmingContestMedalIcon", "inventory.jacobFarmingContests.medalIcon")
    }
}
