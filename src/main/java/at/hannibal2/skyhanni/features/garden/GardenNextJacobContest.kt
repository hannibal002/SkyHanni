package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.enums.OutsideSBFeature
import at.hannibal2.skyhanni.config.features.garden.NextJacobContestConfig.ShareContestsEntry
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.features.garden.GardenApi.getItemStackCopy
import at.hannibal2.skyhanni.features.garden.contest.EliteDevApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.DialogUtils
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchAll
import at.hannibal2.skyhanni.utils.RegexUtils.matchGroups
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.compat.EnchantmentsCompat
import at.hannibal2.skyhanni.utils.json.toJsonArray
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.renderBounds
import at.hannibal2.skyhanni.utils.renderables.StringRenderable
import at.hannibal2.skyhanni.utils.renderables.item.ItemStackRenderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GardenNextJacobContest {

    private const val CLOSE_TO_NEW_YEAR_TEXT = "§7Close to new SB year!"
    private const val MAX_CONTESTS_PER_YEAR = 124
    private val profileStorage get() = SkyHanniMod.feature.storage
    private val config get() = GardenApi.config.jacobContest.nextContest
    private val patternGroup = RepoPattern.group("garden.nextcontest")
    private val calendarDetector by lazy { InventoryDetector(monthPattern) }
    private val haveAllContests get() = knownContests.size == MAX_CONTESTS_PER_YEAR
    private val nextContest get() = knownContests.filterNot {
        it.endTime.isInPast()
    }.minByOrNull { it.endTime }

    private var display: Renderable? = null
    private var simpleDisplay = emptyList<String>()
    private var knownContests: List<EliteDevApi.EliteFarmingContest> = listOf()
    private var nextContestsAvailableAt = SimpleTimeMark.farPast()
    private var lastFetchAttempted = SimpleTimeMark.farPast()
    private var lastWarningTime = SimpleTimeMark.farPast()
    private var loadedContestsYear = -1
    private var isFetchingContests = false
    private var fetchedFromElite = false
    private var isSendingContests = false

    fun isNextCrop(cropName: CropType) = nextContest?.let { contest ->
        contest.crops.contains(cropName) && config.otherGuis
    } ?: false

    fun resetContestData() {
        knownContests = listOf()
        fetchedFromElite = false
        lastFetchAttempted = SimpleTimeMark.farPast()
        fetchContestsIfAble()
    }

    /**
     * REGEX-TEST: §aDay 1
     * REGEX-TEST: §aDay 31
     */
    val dayPattern by patternGroup.pattern(
        "day",
        "§aDay (?<day>.*)",
    )

    /**
     * REGEX-TEST: Early Spring, Year 351
     * REGEX-TEST: Late Summer, Year 351
     * REGEX-TEST: Autumn, Year 351
     */
    val monthPattern by patternGroup.pattern(
        "month",
        "(?<month>(?:\\w+ )?(?:Summer|Spring|Winter|Autumn)), Year (?<year>\\d+)",
    )

    // This pattern covers both the tab list widget, and calendar item lore.
    /**
     * REGEX-TEST: §e○ §7Cactus
     * REGEX-TEST: §6☘ §7Carrot
     * REGEX-TEST: §e○ §7Melon
     * REGEX-TEST:  §r§6☘ §r§fMushroom
     * REGEX-TEST:  §r§e○ §r§fPumpkin
     * REGEX-TEST:  §r§e○ §r§fWheat
     */
    private val cropPattern by patternGroup.pattern(
        "crop",
        " ?(?:§.)*(?:○|(?<boosted>☘)) (?:§.)*(?<crop>.*)",
    )

    /**
     * REGEX-TEST: §e§lJacob's Contest: §r§a19m left
     * REGEX-TEST: §e§lJacob's Contest: §r§a8m left
     */
    private val timeLeftPattern by patternGroup.pattern(
        "time-left",
        "(?:§.)+Jacob's Contest: (?:§.)+(?<timeleft>\\d+[smh]+) left"
    )

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Garden Next Jacob Contest")

        if (!GardenApi.inGarden()) {
            event.addIrrelevant("not in garden")
            return
        }

        event.addIrrelevant {
            add("Current time: ${SimpleTimeMark.now()}")
            add("")

            // TODO Renderable.toString()
            add("Display: '$display'")
            add("")

            nextContest?.let { contest ->
                add("Next Contest:")
                add("  End Time: ${contest.endTime}")
                add("  Crops: ${contest.crops.joinToString(", ") { it.cropName }}")
                add("  Boosted Crop: ${contest.boostedCrop?.cropName ?: "None"}")
            } ?: run {
                add("No upcoming contest found.")
            }
            add("")

            add("Contests:")
            for (contest in knownContests) {
                val time = contest.endTime
                val passedSince = time.passedSince()
                val timeUntil = time.timeUntil()
                val crops = contest.crops
                val recently = 0.seconds..2.hours
                if (passedSince in recently || timeUntil in recently) {
                    add(" Time: $time")
                    if (passedSince.isPositive()) {
                        add("  Passed since: $passedSince")
                    }
                    if (timeUntil.isPositive()) {
                        add("  Time until: $timeUntil")
                    }
                    add("  Crops: $crops")
                }
            }
        }
    }

    @HandleEvent
    fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.JACOB_CONTEST)) return
        simpleDisplay = buildList {
            addAll(event.lines)
            if (isCloseToNewYear()) {
                add(CLOSE_TO_NEW_YEAR_TEXT)
            } else {
                add("§cOpen calendar for")
                add("§cmore exact data!")
            }
        }
        event.tryUpdateBoostedCrop()
    }

    private fun WidgetUpdateEvent.tryUpdateBoostedCrop() {
        nextContest ?: return
        val firstLine = lines.firstOrNull() ?: return
        if (timeLeftPattern.matches(firstLine)) return
        cropPattern.matchAll(lines) {
            if (groupOrNull("boosted") == null) return@matchAll
            val cropType = CropType.getByNameOrNull(groupOrNull("crop") ?: return@matchAll)
            nextContest?.boostedCrop = cropType
        }
    }

    @HandleEvent(SecondPassedEvent::class)
    fun onSecondPassed() {
        if (!isEnabled() || calendarDetector.isInside()) return
        update()
    }

    @HandleEvent(InventoryCloseEvent::class, onlyOnIsland = IslandType.GARDEN)
    fun onInventoryClose() {
        if (!isEnabled()) return
        update()
    }

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!isEnabled() || !calendarDetector.isInside()) return
        val (monthGroup, yearGroup) = monthPattern.matchGroups(
            event.inventoryName,
            "month", "year",
        ) ?: return
        val month = monthGroup?.let(SkyBlockTime::getSBMonthByName) ?: return
        val year = yearGroup?.toIntOrNull() ?: return

        readCalendar(event.inventoryItems.values, year, month)
    }

    private fun readCalendar(items: Collection<ItemStack>, year: Int, month: Int) {
        if (knownContests.isNotEmpty() && loadedContestsYear != year) {
            val endTime = knownContests.first().endTime
            val lastYear = endTime.toSkyBlockTime().year
            if (year != lastYear) knownContests = listOf()
            if (nextContestsAvailableAt.isInFuture() || nextContestsAvailableAt.isFarPast()) {
                nextContestsAvailableAt = SimpleTimeMark.now() - 1.milliseconds
                fetchContestsIfAble()
            }
        }

        // Skip if contests are already loaded for this year
        if (haveAllContests) return

        val contestsOnPage = items.mapNotNull { item ->
            val lore = item.getLore()
            if (!lore.any { it.contains("§6§eJacob's Farming Contest") }) return@mapNotNull null

            val day = dayPattern.matchMatcher(item.displayName) {
                group("day").toInt()
            } ?: return@mapNotNull null

            val startTime = SkyBlockTime(year, month, day).toTimeMark()
            var boostedCrop: CropType? = null
            val crops = lore.mapNotNull { line ->
                cropPattern.matchMatcher(line) {
                    val cropType = groupOrNull("crop")?.let(CropType::getByName)
                    if (groupOrNull("boosted") != null) {
                        boostedCrop = cropType
                    }
                    cropType
                }
            }.takeIfNotEmpty() ?: return@mapNotNull null

            EliteDevApi.EliteFarmingContest(startTime, crops, boostedCrop)
        }

        knownContests = knownContests + contestsOnPage.filter {
            it.startTime !in knownContests.map { contest -> contest.startTime }
        }.sortedBy { it.startTime }

        // If contests were just fully saved
        if (haveAllContests) onHaveAllContests()
        update()
        saveKnownContests()
    }

    private fun isCloseToNewYear(): Boolean {
        val now = SkyBlockTime.now()
        val newYear = SkyBlockTime(year = now.year).toTimeMark()
        val nextYear = SkyBlockTime(year = now.year + 1).toTimeMark()
        val diffA = now.toTimeMark() - newYear
        val diffB = nextYear - now.toTimeMark()

        return diffA < 30.minutes || diffB < 30.minutes
    }

    private fun onHaveAllContests() {
        nextContestsAvailableAt = SkyBlockTime(SkyBlockTime.now().year + 1, 1, 2).toTimeMark()
        if (!isSendEnabled()) return
        if (config.shareAutomatically == ShareContestsEntry.ASK) {
            ChatUtils.clickableChat(
                "§2Click here to submit this year's farming contests. Thank you for helping everyone out!",
                onClick = { shareContests() },
                "§eClick to submit!",
                oneTimeClick = true,
            )
        } else sendContestsIfAble()
    }

    private fun saveKnownContests() {
        val currentYear = SkyBlockTime.now().year
        SkyHanniMod.jacobContestsData.knownContests = knownContests.filter {
            it.endTime.toSkyBlockTime().year == currentYear
        }
        SkyHanniMod.configManager.saveConfig(ConfigFileType.JACOB_CONTESTS, "Save contests")
    }

    @HandleEvent(ConfigLoadEvent::class)
    fun onConfigLoad() {
        val savedContests = SkyHanniMod.jacobContestsData.knownContests
        val savedYear = savedContests.firstOrNull()?.endTime?.toSkyBlockTime()?.year ?: return
        // Clear contests if from previous year
        if (savedYear != SkyBlockTime.now().year) {
            SkyHanniMod.jacobContestsData.knownContests = listOf()
        } else knownContests = savedContests
    }

    private fun shareContests() {
        if (haveAllContests) sendContestsIfAble()
        if (profileStorage.contestSendingAsked || config.shareAutomatically != ShareContestsEntry.ASK) return

        ChatUtils.clickableChat(
            "§2Click here to automatically share future contests!",
            onClick = {
                config.shareAutomatically = ShareContestsEntry.AUTO
                SkyHanniMod.feature.storage.contestSendingAsked = true
                ChatUtils.chat("§2Enabled automatic sharing of future contests!")
            },
            "§eClick to enable autosharing!",
            oneTimeClick = true,
        )
    }

    private fun update() {
        if (nextContestsAvailableAt.isFarPast()) {
            val currentDate = SkyBlockTime.now()
            if (currentDate.month <= 1 && currentDate.day <= 1) {
                nextContestsAvailableAt = SkyBlockTime(SkyBlockTime.now().year + 1, 1, 1).toTimeMark()
            }
        }

        display = if (isFetchingContests) {
            StringRenderable("§cFetching this years jacob contests...")
        } else {
            fetchContestsIfAble() // Will only run when needed/enabled
            drawDisplay()
        }
    }

    private fun drawDisplay() = Renderable.line {
        val nextContest = nextContest
        if (calendarDetector.isInside()) return@line drawCalendarDisplay()
        else if (knownContests.isEmpty()) return@line drawNoContestsDisplay()
        else if (nextContest != null) return@line drawNextContest(nextContest)

        // We only reach here if there are no contests available
        if (isCloseToNewYear()) addString(CLOSE_TO_NEW_YEAR_TEXT)
        else addString("§cOpen calendar to read Jacob contest times!")
        resetContestData()
    }

    private fun MutableList<Renderable>.drawCalendarDisplay() {
        val percentage = knownContests.size.toDouble() / MAX_CONTESTS_PER_YEAR
        val formatted = percentage.formatPercentage()
        addString("§eDetected $formatted of farming contests this year")
    }

    private fun MutableList<Renderable>.drawNoContestsDisplay() =
        if (isCloseToNewYear()) addString(CLOSE_TO_NEW_YEAR_TEXT)
        else addString("§cOpen calendar to read Jacob contest times!")

    private fun MutableList<Renderable>.drawNextContest(contest: EliteDevApi.EliteFarmingContest) {
        val activeContest = contest.startTime.isInPast() && contest.endTime.isInFuture()
        val untilEnd = contest.endTime.timeUntil()
        val duration = when {
            untilEnd > (SkyBlockTime.SKYBLOCK_DAY_MILLIS * 4).milliseconds -> {
                return addString(CLOSE_TO_NEW_YEAR_TEXT)
            }

            activeContest -> {
                addString("§aActive: ")
                untilEnd
            }

            else -> {
                addString("§eNext: ")
                warn(contest.startTime.timeUntil(), contest.crops, contest.boostedCrop)
                contest.startTime.timeUntil()
            }
        }

        for (crop in contest.crops) {
            val isBoosted = crop == contest.boostedCrop
            val cropStack = crop.getItemStackCopy("garden_next_jacob:$crop-$isBoosted-$activeContest").apply {
                if (isBoosted) addEnchantment(EnchantmentsCompat.PROTECTION.enchantment, 1)
            }
            val stack = ItemStackRenderable(cropStack, 1.0)
            if (config.additionalBoostedHighlight && isBoosted) {
                add(stack.renderBounds(config.additionalBoostedHighlightColor.toColor()))
            } else add(stack)
        }

        addString("§7(§b${duration.format()}§7)")
    }

    private fun warn(duration: Duration, crops: List<CropType>, boostedCrop: CropType?) {
        if (!config.warn || config.warnTime.seconds <= duration) return
        val nextContest = nextContest ?: return
        if (nextContest.crops.none { it in config.warnFor }) return

        // Check that it only gets called once for the current event
        if (lastWarningTime.passedSince() < config.warnTime.seconds) return
        lastWarningTime = SimpleTimeMark.now()

        val cropText = crops.joinToString("§7, ") { (if (it == boostedCrop) "§6" else "§a") + it.cropName }
        ChatUtils.chat("Next farming contest: $cropText")
        TitleManager.sendTitle("§eFarming Contest!")
        SoundUtils.playBeepSound()

        val cropTextNoColor = crops.joinToString(", ") {
            if (it == boostedCrop) "<b>${it.cropName}</b>" else it.cropName
        }
        if (config.warnPopup && !Minecraft.getMinecraft().inGameHasFocus) {
            SkyHanniMod.coroutineScope.launch {
                DialogUtils.openPopupWindow(
                    title = "SkyHanni Jacob Contest Notification",
                    message = "<html>Farming Contest soon!<br />Crops: $cropTextNoColor</html>",
                )
            }
        }
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        with(config.pos) {
            if (display == null) renderStrings(simpleDisplay, posLabel = "Next Jacob Contest")
            else renderRenderable(display, posLabel = "Next Jacob Contest")
        }
    }

    @HandleEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!config.display || !calendarDetector.isInside()) return

        SkyHanniMod.feature.misc.inventoryLoadPos.renderRenderable(
            display ?: return,
            posLabel = "Load SkyBlock Calendar",
        )
    }

    private fun sbEnabled() = SkyBlockUtils.inSkyBlock && (GardenApi.inGarden() || config.showOutsideGarden)
    private fun outsideSbEnabled() = OutsideSBFeature.NEXT_JACOB_CONTEST.isSelected() && !SkyBlockUtils.inSkyBlock
    private fun isEnabled() = config.display && (sbEnabled() || outsideSbEnabled())
    private fun isFetchEnabled() = isEnabled() && config.fetchAutomatically
    private fun isSendEnabled() = isFetchEnabled() && config.shareAutomatically != ShareContestsEntry.DISABLED

    private fun fetchContestsIfAble() {
        if (haveAllContests || isFetchingContests || !isFetchEnabled()) return

        // Allows retries every 10 minutes when it's after 1 day into the new year
        if (lastFetchAttempted.passedSince() < 10.minutes || nextContestsAvailableAt.isInFuture()) return

        isFetchingContests = true
        SkyHanniMod.launchIOCoroutine {
            knownContests = EliteDevApi.fetchUpcomingContests().orEmpty()
            if (haveAllContests) {
                ChatUtils.chat("Successfully loaded this year's contests from elitebot.dev automatically!")
                fetchedFromElite = true
                nextContestsAvailableAt = SkyBlockTime(SkyBlockTime.now().year + 1, 1, 2).toTimeMark()
                loadedContestsYear = SkyBlockTime.now().year
            }
            lastFetchAttempted = SimpleTimeMark.now()
            isFetchingContests = false
        }
    }

    private fun sendContestsIfAble() {
        if (isSendingContests || !haveAllContests || isCloseToNewYear()) return
        isSendingContests = true
        SkyHanniMod.launchIOCoroutine {
            EliteDevApi.submitContests(knownContests)
            isSendingContests = false
        }
    }

    @HandleEvent
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

        event.move(18, "garden.nextJacobContests.everywhere", "garden.nextJacobContests.showOutsideGarden")
        event.move(33, "garden.jacobContextTimesPos", "garden.jacobContestTimesPosition")
        event.move(33, "garden.jacobContextTimes", "garden.jacobContestTimes")
        event.move(33, "garden.everywhere", "garden.outsideGarden")
        event.transform(33, "misc.showOutsideSB") { element ->
            element.asJsonArray.map { setting ->
                if (setting.asString == "NEXT_JACOB_CONTEXT") JsonPrimitive("NEXT_JACOB_CONTEST") else setting
            }.toJsonArray()
        }
    }
}
