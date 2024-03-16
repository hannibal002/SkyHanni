package at.hannibal2.skyhanni.features.garden.contest

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.features.garden.GardenNextJacobContest
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.InventoryUtils.getUpperItems
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.NumberUtil.addSuffix
import at.hannibal2.skyhanni.utils.RenderUtils.drawSlotText
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import io.github.moulberry.notenoughupdates.events.SlotClickEvent
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

class JacobFarmingContestsInventory {

    private val realTime = mutableMapOf<Int, String>()

    private val formatDay = SimpleDateFormat("dd MMMM yyyy", Locale.US)
    private val formatTime = SimpleDateFormat("HH:mm", Locale.US)
    private val config get() = SkyHanniMod.feature.inventory.jacobFarmingContests

    // Render the contests a tick delayed to feel smoother
    private var hideEverything = true
    private val medalPattern by RepoPattern.pattern(
        "garden.jacob.contests.inventory.medal",
        "§7§7You placed in the (?<medal>.*) §7bracket!"
    )

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

            foundEvents.add(item.name)
            val time = FarmingContestAPI.getSbTimeFor(item.name) ?: continue
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
    fun onSlotClick(event: SlotClickEvent) {
        if (!config.openOnline.isKeyHeld()) return
        if (!LorenzUtils.inSkyBlock) return
        val invName = InventoryUtils.openInventoryName()
        val name = event.slot.stack.name

        when (invName) {
            "Your Contests" -> {
                FarmingContestAPI.timePattern.matchMatcher(name) {
                    val date = getDate(group("year"), group("month"), group("day"))
                    OSUtils.openBrowser("https://elitebot.dev/contests/$date")
                    ChatUtils.chat("Opening contest in elitebot.dev")
                    event.isCanceled = true
                }
            }
            "Jacob's Farming Contests" -> {
                when (name) {
                    "§6Upcoming Contests" -> {
                        OSUtils.openBrowser("https://elitebot.dev/contests/upcoming")
                        ChatUtils.chat("Opening upcoming contests in elitebot.dev")
                    }
                    "§bClaim your rewards!" -> {
                        OSUtils.openBrowser("https://elitebot.dev/@${LorenzUtils.getPlayerName()}/${HypixelData.profileName}/contests")
                        ChatUtils.chat("Opening your contests in elitebot.dev")
                    }
                    "§aWhat is this?" -> {
                        OSUtils.openBrowser("https://elitebot.dev/contests")
                        ChatUtils.chat("Opening contest page in elitebot.dev")
                    }
                    else -> return
                }
                event.isCanceled = true
            }
            else -> {
                GardenNextJacobContest.monthPattern.matchMatcher(invName) {
                    if (!event.slot.stack.getLore().any { it.contains("§eJacob's Farming Contest") }) return

                    val day = GardenNextJacobContest.dayPattern.matchMatcher(name) { group("day") } ?: return
                    val year = group("year")
                    val month = group("month")
                    val time = SkyBlockTime(year.toInt(), LorenzUtils.getSBMonthByName(month), day.toInt()).toMillis()
                    if (time < SkyBlockTime.now().toMillis()) {
                        val date = getDate(year, month, day)
                        OSUtils.openBrowser("https://elitebot.dev/contests/$date")
                        ChatUtils.chat("Opening contest in elitebot.dev")
                    } else {
                        val highlightText = URLEncoder.encode("$month ${day.toInt().addSuffix()}, Year $year", "UTF-8").replace("+", "%20")
                        OSUtils.openBrowser("https://elitebot.dev/contests/upcoming#:~:text=$highlightText")
                        println("highlight: $highlightText")
                        ChatUtils.chat("Opening upcoming contests in elitebot.dev")
                    }
                    event.isCanceled = true
                }
            }
        }
    }

    private fun getDate(year: String, month: String, day: String) = "$year/${LorenzUtils.getSBMonthByName(month)}/$day"

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

        for ((slot, stack) in chest.getUpperItems()) {
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

            val name = medalPattern.matchMatcher(line) { group("medal").removeColor() } ?: continue
            val medal = LorenzUtils.enumValueOfOrNull<ContestBracket>(name) ?: return

            var stackTip = "§${medal.color}✦"
            var x = event.x + 9
            var y = event.y + 1
            var scale = .7f

            if (finneganContest && config.finneganIcon) {
                stackTip = "§${medal.color}▲"
                x = event.x + 5
                y = event.y - 2
                scale = 1.3f
            }

            event.drawSlotText(x, y, stackTip, scale)
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
