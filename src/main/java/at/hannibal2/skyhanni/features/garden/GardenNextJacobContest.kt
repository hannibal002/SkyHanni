package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI.addCropIcon
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderSingleLineWithItems
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils
import com.google.gson.Gson
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.Display
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.time.Instant
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.UIManager
import kotlin.time.Duration.Companion.seconds

object GardenNextJacobContest {
    private var dispatcher = Dispatchers.IO
    private var display = emptyList<Any>()
    private var simpleDisplay = emptyList<String>()
    private var contests = mutableMapOf<Long, FarmingContest>()
    private var inCalendar = false
    private val patternDay = "§aDay (?<day>.*)".toPattern()
    private val patternMonth = "(?<month>.*), Year (?<year>.*)".toPattern()
    private val patternCrop = "§(e○|6☘) §7(?<crop>.*)".toPattern()

    private const val maxContestsPerYear = 124
    private const val contestDuration = 1_000 * 60 * 20
    private var lastWarningTime = 0L
    private var loadedContestsYear = -1
    private var nextContestsAvailableAt = -1L

    private var lastFetchAttempted = 0L
    private var isFetchingContests = false
    private var fetchedFromElite = false
    private var isSendingContests = false

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        var next = false
        val newList = mutableListOf<String>()
        var counter = 0
        for (line in event.tabList) {
            if (line == "§e§lJacob's Contest:") {
                newList.add(line)
                next = true
                continue
            }
            if (next) {
                if (line == "") break
                newList.add(line)
                counter++
                if (counter == 4) break
            }
        }
        newList.add("§cOpen calendar for")
        newList.add("§cmore exact data!")

        simpleDisplay = newList
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!event.repeatSeconds(2)) return

        if (inCalendar) return
        update()
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (inCalendar) {
            inCalendar = false
            update()
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!config.display) return

        val backItem = event.inventoryItems[48] ?: return
        val backName = backItem.name
        if (backName != "§aGo Back") return
        val lore = backItem.getLore()
        if (lore.size != 1) return
        if (lore[0] != "§7To Calendar and Events") return

        inCalendar = true

        patternMonth.matchMatcher(event.inventoryName) {
            val month = LorenzUtils.getSBMonthByName(group("month"))
            val year = group("year").toInt()

            readCalendar(event.inventoryItems.values, year, month)
        }
    }

    private fun readCalendar(items: Collection<ItemStack>, year: Int, month: Int) {
        if (contests.isNotEmpty() && loadedContestsYear != year) {
            val endTime = contests.values.first().endTime
            val lastYear = SkyBlockTime.fromInstant(Instant.ofEpochMilli(endTime)).year
            if (year != lastYear) {
                contests.clear()
            }
            // Contests are available now, make sure system knows this
            if (nextContestsAvailableAt > System.currentTimeMillis()) {
                nextContestsAvailableAt = System.currentTimeMillis() - 1
                fetchContestsIfAble()
            }
            if (nextContestsAvailableAt == -1L) {
                nextContestsAvailableAt = System.currentTimeMillis() - 1
                fetchContestsIfAble()
            }
        }

        // Skip if contests are already loaded for this year
        if (contests.size == maxContestsPerYear) return

        // Manually loading contests
        for (item in items) {
            val lore = item.getLore()
            if (!lore.any { it.contains("§6§eJacob's Farming Contest") }) continue

            val name = item.name ?: continue
            val day = patternDay.matchMatcher(name) { group("day").toInt() } ?: continue

            val startTime = SkyBlockTime(year, month, day).toMillis()

            val crops = mutableListOf<CropType>()
            for (line in lore) {
                patternCrop.matchMatcher(line) { crops.add(CropType.getByName(group("crop"))) }
            }

            contests[startTime] = FarmingContest(startTime + contestDuration, crops)
        }

        // If contests were just fully saved
        if (contests.size == maxContestsPerYear) {
            nextContestsAvailableAt = SkyBlockTime(SkyBlockTime.now().year + 1, 1, 2).toMillis()

            if (isSendEnabled()) {
                if (!askToSendContests()) {
                    sendContests()
                } else {
                    LorenzUtils.clickableChat(
                        "§e[SkyHanni] §2Click here to submit this years farming contests, thank you for helping everyone out!",
                        "shsendcontests"
                    )
                }
            }
        }
        update()
        saveConfig()
    }

    private fun saveConfig() {
        val map = SkyHanniMod.feature.storage.gardenJacobFarmingContestTimes
        map.clear()

        val currentYear = SkyBlockTime.now().year
        for (contest in contests.values) {
            val contestYear = (SkyBlockTime.fromInstant(Instant.ofEpochMilli(contest.endTime))).year
            // Ensure all stored contests are really from the current year
            if (contestYear != currentYear) continue

            map[contest.endTime] = contest.crops
        }
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        val savedContests = SkyHanniMod.feature.storage.gardenJacobFarmingContestTimes
        val year = savedContests.firstNotNullOfOrNull {
            val endTime = it.key

            SkyBlockTime.fromInstant(Instant.ofEpochMilli(endTime)).year
        }

        // Clear contests if from previous year
        if (year != SkyBlockTime.now().year) {
            savedContests.clear()
        } else {
            for ((time, crops) in savedContests) {
                contests[time] = FarmingContest(time, crops)
            }
        }
    }

    fun shareContestConfirmed(array: Array<String>) {
        if (array.size == 1) {
            if (array[0] == "enable") {
                config.shareAutomatically = 1
                SkyHanniMod.feature.storage.contestSendingAsked = true
                LorenzUtils.chat("§e[SkyHanni] §2Enabled automatic sharing of future contests!")
            }
            return
        }
        if (contests.size == maxContestsPerYear) {
            sendContests()
        }
        if (!SkyHanniMod.feature.storage.contestSendingAsked && config.shareAutomatically == 0) {
            LorenzUtils.clickableChat(
                "§e[SkyHanni] §2Click here to automatically share future contests!",
                "shsendcontests enable"
            )
        }
    }

    class FarmingContest(val endTime: Long, val crops: List<CropType>)

    private fun update() {
        nextContestCrops.clear()

        if (nextContestsAvailableAt == -1L) {
            val currentDate = SkyBlockTime.now()
            if (currentDate.month <= 1 && currentDate.day <= 1) {
                nextContestsAvailableAt = SkyBlockTime(SkyBlockTime.now().year + 1, 1, 1).toMillis()
            }
        }

        display = if (isFetchingContests) {
            listOf("§cFetching this years jacob contests...")
        } else {
            fetchContestsIfAble() // Will only run when needed/enabled
            drawDisplay()
        }
    }

    private fun drawDisplay(): List<Any> {
        val list = mutableListOf<Any>()

        if (inCalendar) {
            val size = contests.size
            val percentage = size.toDouble() / maxContestsPerYear
            val formatted = LorenzUtils.formatPercentage(percentage)
            list.add("§eDetected $formatted of farming contests this year")

            return list
        }

        if (contests.isEmpty()) {
            list.add("§cOpen calendar to read jacob contest times!")
            return list
        }

        val nextContest =
            contests.filter { it.value.endTime > System.currentTimeMillis() }.toSortedMap()
                .firstNotNullOfOrNull { it.value }
        // Show next contest
        if (nextContest != null) return drawNextContest(nextContest, list)

        if (contests.size == maxContestsPerYear) {
            list.add("§cNew SkyBlock Year! Open calendar again!")
        } else {
            list.add("§cOpen calendar to read jacob contest times!")
        }

        fetchedFromElite = false
        contests.clear()

        return list
    }

    private fun drawNextContest(
        nextContest: FarmingContest,
        list: MutableList<Any>,
    ): MutableList<Any> {
        var duration = nextContest.endTime - System.currentTimeMillis()
        if (duration < contestDuration) {
            list.add("§aActive: ")
        } else {
            list.add("§eNext: ")
            duration -= contestDuration
            warn(duration, nextContest.crops)
        }
        for (crop in nextContest.crops) {
            list.add(" ")
            list.addCropIcon(crop)
            nextContestCrops.add(crop)
        }
        val format = TimeUtils.formatDuration(duration)
        list.add("§7(§b$format§7)")

        return list
    }

    private fun warn(timeInMillis: Long, crops: List<CropType>) {
        if (!config.warn) return
        if (config.warnTime <= timeInMillis / 1000) return

        if (System.currentTimeMillis() < lastWarningTime) return
        lastWarningTime = System.currentTimeMillis() + 60_000 * 40

        val cropText = crops.joinToString("§7, ") { "§a${it.cropName}" }
        LorenzUtils.chat("§e[SkyHanni] Next farming contest: $cropText")
        LorenzUtils.sendTitle("§eFarming Contest!", 5.seconds)
        SoundUtils.playBeepSound()

        if (config.warnPopup && !Display.isActive()) {
            SkyHanniMod.coroutineScope.launch {
                openPopupWindow(
                    "Farming Contest soon!\n" +
                            "Crops: ${cropText.removeColor()}"
                )
            }
        }
    }

    /**
     * Taken and modified from Skytils
     */
    private fun openPopupWindow(message: String) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        val frame = JFrame()
        frame.isUndecorated = true
        frame.isAlwaysOnTop = true
        frame.setLocationRelativeTo(null)
        frame.isVisible = true

        val buttons = mutableListOf<JButton>()
        val close = JButton("Ok")
        close.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                frame.isVisible = false
            }
        })
        buttons.add(close)

        val allOptions = buttons.toTypedArray()
        JOptionPane.showOptionDialog(
            frame,
            message,
            "SkyHanni Jacob Contest Notification",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            allOptions,
            allOptions[0]
        )
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        if (display.isEmpty()) {
            config.pos.renderStrings(simpleDisplay, posLabel = "Garden Next Jacob Contest")
        } else {
            config.pos.renderSingleLineWithItems(display, 1.7, posLabel = "Garden Next Jacob Contest")
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!config.display) return
        if (!inCalendar) return

        if (display.isNotEmpty()) {
            SkyHanniMod.feature.misc.inventoryLoadPos.renderSingleLineWithItems(
                display,
                posLabel = "Load SkyBlock Calendar"
            )
        }
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.display
            && (GardenAPI.inGarden() || config.everywhere)

    private fun isFetchEnabled() = isEnabled() && config.fetchAutomatically
    private fun isSendEnabled() = isFetchEnabled() && config.shareAutomatically != 2 // 2 = Disabled
    private fun askToSendContests() =
        config.shareAutomatically == 0 // 0 = Ask, 1 = Send (Only call if isSendEnabled())

    private fun fetchContestsIfAble() {
        if (isFetchingContests || contests.size == maxContestsPerYear || !isFetchEnabled()) return
        // Allows retries every 10 minutes when it's after 1 day into the new year
        val currentMills = System.currentTimeMillis()
        if (lastFetchAttempted + 600_000 > currentMills || currentMills < nextContestsAvailableAt) return

        isFetchingContests = true

        SkyHanniMod.coroutineScope.launch {
            fetchUpcomingContests()
            lastFetchAttempted = System.currentTimeMillis()
            isFetchingContests = false
        }
    }

    private suspend fun fetchUpcomingContests() {
        try {
            val url = "https://api.elitebot.dev/contests/at/now"
            val result = withContext(dispatcher) { APIUtil.getJSONResponse(url) }.asJsonObject

            val newContests = mutableMapOf<Long, FarmingContest>()

            val complete = result["complete"].asBoolean
            if (complete) {
                for (entry in result["contests"].asJsonObject.entrySet()) {
                    var timestamp = entry.key.toLongOrNull() ?: continue
                    timestamp *= 1_000 // Seconds to milliseconds

                    val crops = entry.value.asJsonArray.map {
                        CropType.getByName(it.asString)
                    }

                    if (crops.size != 3) continue

                    newContests[timestamp + contestDuration] = FarmingContest(timestamp + contestDuration, crops)
                }
            } else {
                LorenzUtils.chat("§e[SkyHanni] This years contests aren't available to fetch automatically yet, please load them from your calender or wait 10 minutes!")
            }

            if (newContests.count() == maxContestsPerYear) {
                LorenzUtils.chat("§e[SkyHanni] Successfully loaded this year's contests from elitebot.dev automatically!")

                contests = newContests
                fetchedFromElite = true
                nextContestsAvailableAt = SkyBlockTime(SkyBlockTime.now().year + 1, 1, 2).toMillis()
                loadedContestsYear = SkyBlockTime.now().year

                saveConfig()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            LorenzUtils.error("[SkyHanni] Failed to fetch upcoming contests. Please report this error if it continues to occur.")
        }
    }

    private fun sendContests() {
        if (isSendingContests || contests.size != maxContestsPerYear) return

        isSendingContests = true

        SkyHanniMod.coroutineScope.launch {
            submitContestsToElite()
            isSendingContests = false
        }
    }

    private suspend fun submitContestsToElite() = try {
        val formatted = mutableMapOf<Long, List<String>>()

        for ((endTime, contest) in contests) {
            formatted[endTime / 1000] = contest.crops.map {
                it.cropName
            }
        }

        val url = "https://api.elitebot.dev/contests/at/now"
        val body = Gson().toJson(formatted)

        val result = withContext(dispatcher) { APIUtil.postJSONIsSuccessful(url, body) }

        if (result) {
            LorenzUtils.chat("§e[SkyHanni] Successfully submitted this years upcoming contests, thank you for helping everyone out!")
        } else {
            LorenzUtils.error("[SkyHanni] Something went wrong submitting upcoming contests!")
        }
    } catch (e: Exception) {
        e.printStackTrace()
        LorenzUtils.error("[SkyHanni] Failed to submit upcoming contests. Please report this error if it continues to occur.")
        null
    }

    private val config get() = SkyHanniMod.feature.garden.nextJacobContests
    private val nextContestCrops = mutableListOf<CropType>()

    fun isNextCrop(cropName: CropType) = nextContestCrops.contains(cropName) && config.otherGuis

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.nextJacobContestDisplay", "garden.nextJacobContests.display")
        event.move(3, "garden.nextJacobContestEverywhere", "garden.nextJacobContests.everywhere")
        event.move(3, "garden.nextJacobContestOtherGuis", "garden.nextJacobContests.otherGuis")
        event.move(3, "garden.nextJacobContestsFetchAutomatically", "garden.nextJacobContests.fetchAutomatically")
        event.move(3, "garden.nextJacobContestsShareAutomatically", "garden.nextJacobContests.shareAutomatically")
        event.move(3, "garden.nextJacobContestWarn", "garden.nextJacobContests.warn")
        event.move(3, "garden.nextJacobContestWarnTime", "garden.nextJacobContests.warnTime")
        event.move(3, "garden.nextJacobContestWarnPopup", "garden.nextJacobContests.warnPopup")
        event.move(3, "garden.nextJacobContestPos", "garden.nextJacobContests.pos")
    }
}
