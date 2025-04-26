package at.hannibal2.skyhanni.features.event.hoppity.summary

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.event.hoppity.summary.HoppityEventSummaryConfig.HoppityStat
import at.hannibal2.skyhanni.config.features.event.hoppity.summary.HoppityLiveDisplayConfig
import at.hannibal2.skyhanni.config.features.event.hoppity.summary.HoppityLiveDisplayConfig.HoppityDateTimeFormat.RELATIVE
import at.hannibal2.skyhanni.config.features.event.hoppity.summary.HoppityLiveDisplayConfig.HoppityLiveDisplayInventoryType
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage.HoppityEventStats
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.features.event.hoppity.HoppityApi
import at.hannibal2.skyhanni.features.event.hoppity.HoppityApi.getEventEndMark
import at.hannibal2.skyhanni.features.event.hoppity.HoppityApi.getEventStartMark
import at.hannibal2.skyhanni.features.event.hoppity.HoppityApi.getHoppityEventNumber
import at.hannibal2.skyhanni.features.event.hoppity.HoppityEggLocator
import at.hannibal2.skyhanni.features.event.hoppity.HoppityEggType
import at.hannibal2.skyhanni.features.event.hoppity.HoppityRabbitTheFishChecker.mealEggInventoryPattern
import at.hannibal2.skyhanni.features.event.hoppity.summary.HoppityEventSummary.StatString
import at.hannibal2.skyhanni.features.event.hoppity.summary.HoppityEventSummary.buildEmptyFallback
import at.hannibal2.skyhanni.features.event.hoppity.summary.HoppityEventSummary.dropConsecutiveEmpties
import at.hannibal2.skyhanni.features.event.hoppity.summary.HoppityEventSummary.getMealEggCounts
import at.hannibal2.skyhanni.features.event.hoppity.summary.HoppityEventSummary.getSpawnedEggCounts
import at.hannibal2.skyhanni.features.event.hoppity.summary.HoppityEventSummary.getYearStats
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.CFApi
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.CFApi.partyModeReplace
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.CFShopPrice.menuNamePattern
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.TimeUtils.getCountdownFormat
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.HorizontalRenderableContainer
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableContainer
import at.hannibal2.skyhanni.utils.renderables.RenderableString
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addCenteredString
import at.hannibal2.skyhanni.utils.renderables.VerticalContainerRenderable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiInventory
import org.lwjgl.input.Keyboard
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

private typealias DTType = HoppityLiveDisplayConfig.HoppityDateTimeDisplayType

@SkyHanniModule
object HoppityLiveDisplay {

    /**
     * REGEX-TEST: Hoppity's Collection
     * REGEX-TEST: (1/2) Hoppity's Collection
     * REGEX-TEST: Chocolate Factory Milestones
     * REGEX-TEST: Chocolate Shop Milestones
     */
    private val miscCfInventoryPatterns by CFApi.patternGroup.pattern(
        "cf.inventory",
        "(?:\\(\\d*\\/\\d*\\) )?Hoppity's Collection|Chocolate (?:Factory|Shop) Milestones|Rabbit Hitman",
    )

    private val config get() = SkyHanniMod.feature.event.hoppityEggs
    private val storage get() = ProfileStorageData.profileSpecific
    private val liveDisplayConfig get() = config.eventSummary.liveDisplay
    private val currentSbYear get() = SkyBlockTime.now().year

    private data class RenderableOverrideOperation(
        val statStrings: List<StatString>,
        val baseRenderable: Renderable,
        val stats: HoppityEventStats,
        val year: Int,
    )

    private val renderableOverridesOperationList by lazy {
        buildMap<HoppityStat, (RenderableOverrideOperation) -> Renderable> {
            put(HoppityStat.MEAL_EGGS_FOUND) { (_, baseRenderable, stats, year) ->
                val hoverTips = stats.buildMealEggHover(year).map { it.partyModeReplace() }
                if (!liveDisplayConfig.mealEggHover || hoverTips.isEmpty()) baseRenderable
                else Renderable.hoverTips(baseRenderable, hoverTips)
            }
        }
    }

    private var lastKnownStatHash = 0
    private var lastKnownInInvState = false
    private var currentTimerActive = false
    private var displayCardRenderables: List<Renderable> = emptyList()
    private var lastToggleMark: SimpleTimeMark = SimpleTimeMark.farPast()

    @HandleEvent(onlyOnSkyblock = true, eventTypes = [InventoryCloseEvent::class, InventoryFullyOpenedEvent::class])
    fun reCheckInventoryState() {
        if (isInInventory() != lastKnownInInvState) {
            lastKnownInInvState = !lastKnownInInvState
            lastKnownStatHash = 0
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed(event: SecondPassedEvent) {
        reCheckInventoryState()
        recheckHashClear(event)
    }

    @HandleEvent
    fun onConfigLoad() {
        config.eventSummary.statDisplayList.afterChange {
            lastKnownStatHash = 0
        }
        CFApi.config.partyMode.afterChange {
            lastKnownStatHash = 0
        }
    }

    @HandleEvent
    fun onKeyPress(event: KeyPressEvent) {
        reCheckInventoryState()
        if (!liveDisplayConfig.enabled) return
        if (liveDisplayConfig.toggleKeybind == Keyboard.KEY_NONE || liveDisplayConfig.toggleKeybind != event.keyCode) return
        // Only toggle from inventory if the user is in the Chocolate Factory
        if (Minecraft.getMinecraft().currentScreen != null && !CFApi.inChocolateFactory) return
        if (lastToggleMark.passedSince() < 250.milliseconds) return
        val storage = storage ?: return
        storage.hoppityStatLiveDisplayToggledOff = !storage.hoppityStatLiveDisplayToggledOff
        lastToggleMark = SimpleTimeMark.now()
    }

    @HandleEvent(GuiRenderEvent::class)
    fun onRenderOverlay() {
        if (!liveDisplayEnabled()) return

        val stats = getYearStats(HoppityEventSummary.statYear) ?: return
        // Calculate a 'hash' of the stats to determine if they have changed
        val statsHash = stats.hashCode()
        if (statsHash != lastKnownStatHash) {
            lastKnownStatHash = statsHash
            displayCardRenderables = buildDisplayRenderables(stats, HoppityEventSummary.statYear)
        }

        config.eventSummary.liveDisplayPosition.renderRenderables(
            displayCardRenderables,
            posLabel = "Hoppity's Hunt Stats",
        )
    }

    private fun liveDisplayEnabled(): Boolean {
        val storage = storage ?: return false
        val isToggledOff = storage.hoppityStatLiveDisplayToggledOff
        val isEnabled = liveDisplayConfig.enabled
        val isIslandEnabled = !liveDisplayConfig.onlyHoppityIslands || HoppityApi.onHoppityIsland()
        val isEventEnabled = !liveDisplayConfig.onlyDuringEvent || HoppityApi.isHoppityEvent()
        val isEggLocatorEnabled = !liveDisplayConfig.mustHoldEggLocator || InventoryUtils.itemInHandId == HoppityEggLocator.locatorItem
        val isInventoryEnabled = liveDisplayConfig.specificInventories.isEmpty() || inMatchingInventory()

        return LorenzUtils.inSkyBlock &&
            !isToggledOff &&
            isEnabled &&
            isIslandEnabled &&
            isEventEnabled &&
            isEggLocatorEnabled &&
            isInventoryEnabled
    }

    private fun inMatchingInventory(): Boolean {
        val setting = liveDisplayConfig.specificInventories
        val currentScreen = Minecraft.getMinecraft().currentScreen ?: return HoppityLiveDisplayInventoryType.NO_INVENTORY in setting

        // Get the inventory name and check if it matches any of the specific inventories
        val inventoryName = InventoryUtils.openInventoryName()

        val inChocolateFactory =
            CFApi.inChocolateFactory ||
                menuNamePattern.matches(inventoryName) ||
                miscCfInventoryPatterns.matches(inventoryName)

        return if (currentScreen is GuiInventory) {
            HoppityLiveDisplayInventoryType.OWN_INVENTORY in setting
        } else if (inChocolateFactory) {
            HoppityLiveDisplayInventoryType.CHOCOLATE_FACTORY in setting
        } else if (inventoryName == "Hoppity") {
            HoppityLiveDisplayInventoryType.HOPPITY in setting
        } else if (mealEggInventoryPattern.matches(inventoryName)) {
            HoppityLiveDisplayInventoryType.MEAL_EGGS in setting
        } else false
    }

    private fun isInInventory(): Boolean =
        Minecraft.getMinecraft().currentScreen is GuiInventory || Minecraft.getMinecraft().currentScreen is GuiChest

    private fun recheckHashClear(event: SecondPassedEvent) {
        if (!currentTimerActive) return
        if (!event.repeatSeconds(liveDisplayConfig.refreshFrequency)) return
        lastKnownStatHash = 0
    }

    private fun HoppityEventStats.buildMealEggHover(statYear: Int): List<String> = buildList {
        val spawnedEggs: Map<HoppityEggType, Int> = getSpawnedEggCounts(statYear).takeIfNotEmpty() ?: return@buildList
        val totalSpawnedEggs = spawnedEggs.values.sum()

        val totalMealsFound = getMealEggCounts().sumAllValues().toInt()
        val (totalPercentFormat, totalColor) = getFoundPercentFormat(totalMealsFound, totalSpawnedEggs)
        val totalColorFormat = totalColor.getChatColor()
        val totalCountFormat = "$totalColorFormat$totalMealsFound§7/§a$totalSpawnedEggs"
        val totalFinalPercentFormat = "§8(§7$totalPercentFormat§8)"
        add("§7Total: $totalCountFormat $totalFinalPercentFormat")
        add("§8${"▬".repeat(16)}")

        spawnedEggs.filter { it.value > 0 }.forEach { (type, spawnCount) ->
            val amountCollected = mealsFound[type] ?: 0
            val (percentFormat, color) = getFoundPercentFormat(amountCollected, spawnCount)
            val colorFormat = color.getChatColor()
            val countFormat = "$colorFormat$amountCollected§7/§a$spawnCount"
            val finalPercentFormat = "§8(§7$percentFormat§8)"
            add("${type.coloredName}§7: $countFormat $finalPercentFormat")
        }
    }

    private fun getFoundPercentFormat(
        amountFound: Int,
        amountTotal: Int
    ): Pair<String, LorenzColor> {
        if (amountTotal == 0) return "0%" to LorenzColor.RED

        val rawPercent = amountFound.toDouble() * 100.0 / amountTotal
        val percentText = "${rawPercent.roundToInt()}%".partyModeReplace()

        val percentageColor = when {
            rawPercent > 90 -> LorenzColor.GREEN
            rawPercent > 50 -> LorenzColor.YELLOW
            else -> LorenzColor.RED
        }
        return percentText to percentageColor
    }

    private fun buildTitle(statYear: Int) = VerticalContainerRenderable(
        buildList {
            addString(
                when (statYear) {
                    Int.MAX_VALUE -> "§dHoppity's Hunt All-Time Stats"
                    else -> "§dHoppity's Hunt #${getHoppityEventNumber(statYear)} Stats"
                }.partyModeReplace(),
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
            )
            if (statYear == Int.MAX_VALUE) {
                val numberEvents = storage?.hoppityEventStats?.keys?.count { it <= currentSbYear } ?: 0
                addCenteredString(
                    "§7Compiled from §f$numberEvents §7events".partyModeReplace(),
                )
                return@buildList
            }

            val eventEnd = getEventEndMark(statYear)
            val isHoppity = HoppityApi.isHoppityEvent()

            val isCurrentEvent = isHoppity && HoppityEventSummary.statYear == currentSbYear
            val isPastEvent = HoppityEventSummary.statYear < currentSbYear || (HoppityEventSummary.statYear == currentSbYear && !isHoppity)

            val configMatches = when {
                isCurrentEvent -> liveDisplayConfig.dateTimeDisplay.contains(DTType.CURRENT)
                isPastEvent -> liveDisplayConfig.dateTimeDisplay.contains(DTType.PAST_EVENTS)
                else -> liveDisplayConfig.dateTimeDisplay.contains(DTType.NEXT_EVENT)
            }
            if (!configMatches) return@buildList

            val (timeMarkFormat, timeMarkAbs) = when {
                isCurrentEvent || isPastEvent -> eventEnd
                else -> getEventStartMark(HoppityEventSummary.statYear)
            }.formatForHoppity()

            val grammarFormat = when {
                isCurrentEvent -> if (timeMarkAbs) "Ends" else "Ends in"
                isPastEvent -> if (timeMarkAbs) "" else " ago"
                else -> if (timeMarkAbs) "Starts" else "Starts in"
            }

            addCenteredString(
                when {
                    isCurrentEvent -> "§7$grammarFormat §f$timeMarkFormat"
                    isPastEvent -> "§7Ended §f$timeMarkFormat$grammarFormat"
                    else -> "§7$grammarFormat §f$timeMarkFormat"
                }.partyModeReplace(),
            )
        },
        horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
    )

    private fun SimpleTimeMark.formatForHoppity(): Pair<String, Boolean> =
        if (SkyHanniMod.feature.event.hoppityEggs.eventSummary.liveDisplay.dateTimeFormat == RELATIVE)
            Pair(passedSince().absoluteValue.format(maxUnits = 2), false)
        else {
            val countDownFormat = toLocalDateTime().getCountdownFormat()
            Pair(formattedDate(countDownFormat), true)
        }

    private fun buildYearSwitcherRenderables(currentStatYear: Int): List<Renderable>? {
        val storage = storage ?: return null
        val statsStorage = storage.hoppityEventStats

        val isNextEventEnabled = liveDisplayConfig.dateTimeDisplay.contains(DTType.NEXT_EVENT)
        val isAllTimeEnabled = liveDisplayConfig.showAllTime

        val isAllTime = currentStatYear == Int.MAX_VALUE
        val nextYear = currentSbYear + 1
        val isAlreadyNextEvent = currentStatYear == nextYear
        val predecessorYear = statsStorage.keys.filter {
            it < currentStatYear &&
                (it != nextYear || isNextEventEnabled)
        }.maxOrNull()
        val successorYear =
            if (isAllTime) null
            else statsStorage.keys.filter { it in (currentStatYear + 1)..<nextYear }.minOrNull()

        val predecessorButton = predecessorYear?.let {
            buildStatYearSwitcher(
                "Hunt #${getHoppityEventNumber(it)}".toLeftButtonString(),
                it,
            )
        }

        val successorButton = successorYear?.let {
            buildStatYearSwitcher(
                "Hunt #${getHoppityEventNumber(it)}".toRightButtonString(),
                it,
            )
        }

        val nextEventButton = buildStatYearSwitcher(
            "Next Hunt".toRightButtonString(),
            nextYear,
        ).takeIf { isNextEventEnabled && !isAlreadyNextEvent && !isAllTime }

        val allTimeButton = buildStatYearSwitcher(
            "All-Time".toRightButtonString(),
            Int.MAX_VALUE,
        ).takeIf { isAllTimeEnabled && !isAllTime }

        val rightButton = successorButton
            ?: nextEventButton
            ?: allTimeButton

        return listOfNotNull(
            predecessorButton,
            rightButton,
        )
    }

    private fun buildDisplayRenderables(stats: HoppityEventStats?, statYear: Int): List<Renderable> = buildList {
        if (stats == null) return@buildList
        currentTimerActive = true

        add(buildTitle(statYear))
        tryAddYearSwitchers(statYear)
        add(stats.getRenderableContainer(statYear))
    }

    private fun MutableList<StatString>.getContainer(): Renderable = VerticalContainerRenderable(
        this.map { RenderableString(it.string) }
    )

    private fun HoppityEventStats.getRenderableContainer(
        statYear: Int,
    ): Renderable = VerticalContainerRenderable(
        HoppityEventSummary.getMappedStatStrings(
            this,
            statYear
        ).mapNotNull { (stat, statStrings) ->
            val baseRenderable = statStrings.dropConsecutiveEmpties().takeIfNotEmpty()?.getContainer()
                ?: return@mapNotNull null
            val intendedOperation = RenderableOverrideOperation(
                statStrings = statStrings,
                baseRenderable = baseRenderable,
                stats = this,
                year = statYear,
            )
            renderableOverridesOperationList[stat]?.invoke(intendedOperation) ?: baseRenderable
        }.toMutableList().let { renderableList ->
            val isCurrentEvent = HoppityApi.isHoppityEvent() && statYear == currentSbYear
            val isEmpty = renderableList.isEmpty() || renderableList.all { it.isEmpty() }

            if (isEmpty) buildEmptyFallback(isCurrentEvent).map {
                RenderableString(it.string)
            } else renderableList
        }
    )

    private fun Renderable.isEmpty(): Boolean = this is RenderableString && text.trim().isEmpty() ||
        this is RenderableContainer && renderables.all { it.isEmpty() }

    private fun MutableList<Renderable>.tryAddYearSwitchers(statYear: Int) {
        if (!isInInventory()) return
        val renderable = buildYearSwitcherRenderables(statYear) ?: return
        val container = HorizontalRenderableContainer(
            renderable,
            spacing = 5,
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
        )
        add(container)
    }

    private fun buildStatYearSwitcher(text: String, year: Int) =
        Renderable.optionalLink(text, onLeftClick = { HoppityEventSummary.statYear = year })

    private fun String.toLeftButtonString() = "§d[ §r§f§l<- §r§7$this §r§d]".partyModeReplace()
    private fun String.toRightButtonString() = "§d[ §7$this §r§f§l-> §r§d]".partyModeReplace()
}
