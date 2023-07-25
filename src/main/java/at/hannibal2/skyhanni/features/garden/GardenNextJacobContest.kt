package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.TitleUtils
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.features.garden.GardenAPI.addCropIcon
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderSingleLineWithItems
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import kotlinx.coroutines.launch
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

class GardenNextJacobContest {
    private var display = emptyList<Any>()
    private var simpleDisplay = emptyList<String>()
    private var contests = mutableMapOf<Long, FarmingContest>()
    private var inCalendar = false
    private val patternDay = "§aDay (?<day>.*)".toPattern()
    private val patternMonth = "(?<month>.*), Year (?<year>.*)".toPattern()
    private val patternCrop = "§e○ §7(?<crop>.*)".toPattern()

    private val maxContestsPerYear = 124
    private val contestDuration = 1_000 * 60 * 20
    private var lastWarningTime = 0L

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
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!config.nextJacobContestDisplay) return

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
        if (contests.isNotEmpty()) {
            val contest = contests.values.first()
            val endTime = contest.endTime
            val lastYear = SkyBlockTime.fromInstant(Instant.ofEpochMilli(endTime)).year
            if (year != lastYear) {
                contests.clear()
                LorenzUtils.chat("§e[SkyHanni] New year detected, open all calendar months again!")
            }
        }

        for (item in items) {
            val lore = item.getLore()
            if (!lore.any { it.contains("§6§eJacob's Farming Contest") }) continue

            val name = item.name ?: continue
            val matcherDay = patternDay.matcher(name)
            if (!matcherDay.matches()) continue

            val day = matcherDay.group("day").toInt()
            val startTime = SkyBlockTime(year, month, day).toMillis()
            val crops = mutableListOf<CropType>()
            for (line in lore) {
                val matcherCrop = patternCrop.matcher(line)
                if (!matcherCrop.matches()) continue
                crops.add(CropType.getByName(matcherCrop.group("crop")))
            }
            val contest = FarmingContest(startTime + contestDuration, crops)
            contests[startTime] = contest
        }

        update()
        saveConfig()
    }

    private fun saveConfig() {
        val map = SkyHanniMod.feature.storage.gardenJacobFarmingContestTimes
        map.clear()
        for (contest in contests.values) {
            map[contest.endTime] = contest.crops
        }
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        for ((time, crops) in SkyHanniMod.feature.storage.gardenJacobFarmingContestTimes) {
            contests[time] = FarmingContest(time, crops)
        }
    }

    class FarmingContest(val endTime: Long, val crops: List<CropType>)

    private fun update() {
        nextContestCrops.clear()
        display = drawDisplay()
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
            return emptyList()
        }

        val nextContest =
            contests.filter { it.value.endTime > System.currentTimeMillis() }.toSortedMap()
                .firstNotNullOfOrNull { it.value }
        if (nextContest == null) {
            if (contests.size == maxContestsPerYear) {
                list.add("§cNew SkyBlock Year! Open calendar again!")
            } else {
                list.add("§cOpen calendar to read jacob contest times!")
            }
            return list
        }

        return drawNextContest(nextContest, list)
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
        if (!config.nextJacobContestWarn) return
        if (config.nextJacobContestWarnTime <= timeInMillis / 1000) return

        if (System.currentTimeMillis() < lastWarningTime) return
        lastWarningTime = System.currentTimeMillis() + 60_000 * 40

        val cropText = crops.joinToString("§7, ") { "§a${it.cropName}" }
        LorenzUtils.chat("§e[SkyHanni] Next farming contest: $cropText")
        TitleUtils.sendTitle("§eFarming Contest!", 5_000)
        SoundUtils.playBeepSound()

        if (config.nextJacobContestWarnPopup && !Display.isActive()) {
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
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return

        if (display.isEmpty()) {
            config.nextJacobContestPos.renderStrings(simpleDisplay, posLabel = "Garden Next Jacob Contest")
        } else {
            config.nextJacobContestPos.renderSingleLineWithItems(display, 1.7, posLabel = "Garden Next Jacob Contest")
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.ChestBackgroundRenderEvent) {
        if (!config.nextJacobContestDisplay) return
        if (!inCalendar) return

        if (display.isNotEmpty()) {
            SkyHanniMod.feature.misc.inventoryLoadPos.renderSingleLineWithItems(
                display,
                posLabel = "Load SkyBlock Calendar"
            )
        }
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.nextJacobContestDisplay
            && (GardenAPI.inGarden() || config.nextJacobContestEverywhere)

    companion object {
        private val config get() = SkyHanniMod.feature.garden
        private val nextContestCrops = mutableListOf<CropType>()

        fun isNextCrop(cropName: CropType) = nextContestCrops.contains(cropName) && config.nextJacobContestOtherGuis
    }
}