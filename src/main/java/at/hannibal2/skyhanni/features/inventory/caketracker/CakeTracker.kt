package at.hannibal2.skyhanni.features.inventory.caketracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.inventory.CakeTrackerConfig.CakeTrackerDisplayOrderType
import at.hannibal2.skyhanni.config.features.inventory.CakeTrackerConfig.CakeTrackerDisplayType
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage.CakeData
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.InventoryUtils.getUpperItems
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.KeyboardManager.LEFT_MOUSE
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.matchGroup
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.TimeLimitedCache
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableString
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableButton
import at.hannibal2.skyhanni.utils.renderables.ScrollValue
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import io.github.notenoughupdates.moulconfig.ChromaColour
import net.minecraft.inventory.ContainerChest
import org.lwjgl.input.Keyboard.KEY_DOWN
import org.lwjgl.input.Keyboard.KEY_LEFT
import org.lwjgl.input.Keyboard.KEY_RIGHT
import org.lwjgl.input.Keyboard.KEY_UP
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

private typealias DisplayOrder = CakeTrackerDisplayOrderType
private typealias DisplayType = CakeTrackerDisplayType

@SkyHanniModule
object CakeTracker {

    private val patternGroup = RepoPattern.group("inventory.cake-tracker")

    // <editor-fold desc="Patterns">
    /**
     * REGEX-TEST: §cNew Year Cake (Year 360)
     * REGEX-TEST: §cNew Year Cake (Year 1,000)
     * REGEX-TEST: §f§f§cNew Year Cake (Year 330)
     * REGEX-TEST: §f§f§cNew Year Cake (Year 49)
     */
    private val cakeNamePattern by patternGroup.pattern(
        "cake.name",
        "(?:§f§f)?§cNew Year Cake \\(Year (?<year>[\\d,]+)\\)",
    )

    /**
     * REGEX-TEST: §eYou purchased §r§f§r§f§r§cNew Year Cake (Year 143) §r§efor §r§62,000,000 coins§r§e!
     */
    private val cakePurchasedPattern by patternGroup.pattern(
        "cake.purchased",
        "§eYou purchased (?:§.)*New Year Cake \\(Year (?<year>[\\d,]+)\\) (?:§.)*for (?:§.)+(?<coins>[\\d,]+) coins(?:§.)+!",
    )

    /**
     * REGEX-TEST: Ender Chest (2/9)
     * REGEX-TEST: Jumbo Backpack (Slot #6)
     * REGEX-TEST: New Year Cake Bag
     * REGEX-TEST: Large Chest
     * REGEX-TEST: Chest
     */
    private val cakeContainerPattern by patternGroup.pattern(
        "cake.container",
        "Ender Chest \\(\\d{1,2}/\\d{1,2}\\)|.*Backpack(?:§r)? \\(Slot #\\d{1,2}\\)|New Year Cake Bag|(?:Large )?Chest",
    )

    /**
     * REGEX-TEST: Auctions Browser
     * REGEX-TEST: Auctions: "Test"
     * REGEX-TEST: Auctions: "New Year Cake (Year
     */
    private val auctionBrowserPattern by patternGroup.pattern(
        "auction.search",
        "Auctions Browser|Auctions: \".*",
    )

    /**
     * REGEX-TEST: Auctions: "New Year C
     */
    private val auctionCakeSearchPattern by patternGroup.pattern(
        "auction.cakesearch",
        "Auctions: \"New Year C.*",
    )

    /**
     * REGEX-TEST: §aYou claimed a §r§cNew Year Cake§r§a!
     */
    private val cakeBakerClaimedPattern by patternGroup.pattern(
        "cake.baker.claimed",
        "§aYou claimed a (?:§.)*New Year Cake(?:§.)*!",
    )
    // </editor-fold>

    private val storage get() = ProfileStorageData.profileSpecific?.cakeData
    private val config get() = SkyHanniMod.feature.inventory.cakeTracker
    private val maxTrackerHeight: Float get() = config.maxHeight.get()
    private val cakeScrollValue = ScrollValue().apply { init(0.0) }
    private val cakePriceCache: TimeLimitedCache<Int, Double> = TimeLimitedCache(5.minutes)
    private val searchOverrideCache: TimeLimitedCache<Pair<Int, Int>, Int> = TimeLimitedCache(5.minutes)
    private val unobtainedHighlightColor: ChromaColour get() = config.unobtainedAuctionHighlightColor.toChromaColor()
    private val obtainedHighlightColor: ChromaColour get() = config.obtainedAuctionHighlightColor.toChromaColor()

    private var currentYear = 0
    private var inCakeInventory = false
    private var timeOpenedCakeInventory = SimpleTimeMark.farPast()
    private var inAuctionHouse = false
    private var slotHighlightCache = mapOf<Int, ChromaColour>()
    private var searchingForCakes = false
    private var knownCakesInCurrentInventory = listOf<Int>()
    private var cakeRenderables = listOf<Renderable>()
    private var lastKnownCakeDataHash = 0

    private fun invalidateCakeCache() {
        lastKnownCakeDataHash = 0
    }

    private fun getSelectedCake(start: Int, end: Int) = if (start == end) start else searchOverrideCache[start to end]

    private fun addCake(cakeYear: Int) {
        val storage = storage ?: return
        val changed = storage.ownedCakes.add(cakeYear)
        if (changed) recalculateMissingCakes()
    }

    private fun removeCake(cakeYear: Int) {
        val storage = storage ?: return
        val changed = storage.ownedCakes.remove(cakeYear)
        if (changed) recalculateMissingCakes()
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shresetcaketracker") {
            description = "Resets the New Year Cake Tracker"
            category = CommandCategory.USERS_RESET
            callback {
                val storage = storage ?: return@callback
                storage.ownedCakes.clear()
                recalculateMissingCakes()
                ChatUtils.chat("New Year Cake tracker data reset")
            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent) {
        if (!config.enabled) return
        cakePurchasedPattern.matchMatcher(event.message) {
            val year = group("year").formatInt()
            addCake(year)
        }
        if (cakeBakerClaimedPattern.matches(event.message)) {
            addCake(currentYear)
        }
    }

    @HandleEvent
    fun onConfigLoad() {
        ConditionalUtils.onToggle(
            config.maxHeight,
            config.displayType,
            config.displayOrderType,
        ) { invalidateCakeCache() }
    }

    @HandleEvent(GuiRenderEvent.ChestGuiOverlayRenderEvent::class, onlyOnSkyblock = true)
    fun onBackgroundDraw() {
        if (!config.enabled) return

        val inInvWithCakes = inCakeInventory && knownCakesInCurrentInventory.any()
        val inAuctionWithCakes = inAuctionHouse && (slotHighlightCache.isNotEmpty() || searchingForCakes)
        if (!inInvWithCakes && !inAuctionWithCakes) return

        reRenderDisplay()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!config.enabled) return
        if (inCakeInventory) checkInventoryCakes()
        if (!inAuctionHouse) return

        (event.container as ContainerChest).getUpperItems().forEach { (slot, _) ->
            slotHighlightCache[slot.slotIndex]?.let { color ->
                slot.highlight(color)
            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!config.enabled) return
        knownCakesInCurrentInventory = listOf()
        checkCakeContainer(event)
        inAuctionHouse = checkAuctionCakes(event)
    }

    private fun reRenderDisplay() {
        config.cakeTrackerPosition.renderRenderables(
            drawDisplay(storage ?: return),
            posLabel = "New Year Cake Tracker",
        )
    }

    private fun checkCakeContainer(event: InventoryFullyOpenedEvent) {
        if (!cakeContainerPattern.matches(event.inventoryName)) return
        knownCakesInCurrentInventory = event.inventoryItems.values.mapNotNull { item ->
            cakeNamePattern.matchMatcher(item.displayName) {
                val year = group("year").formatInt()
                addCake(year)
                year
            }
        }.toMutableList()
        inCakeInventory = true
        timeOpenedCakeInventory = SimpleTimeMark.now()
    }

    private fun checkAuctionCakes(event: InventoryFullyOpenedEvent): Boolean {
        if (!auctionBrowserPattern.matches(event.inventoryName)) return false
        searchingForCakes = auctionCakeSearchPattern.matches(event.inventoryName)
        slotHighlightCache = event.inventoryItems.filter {
            cakeNamePattern.matches(it.value.displayName)
        }.mapValues { (_, item) ->
            val year = cakeNamePattern.matchGroup(item.displayName, "year")?.toInt() ?: -1
            val owned = storage?.ownedCakes?.contains(year) ?: false
            if (owned) obtainedHighlightColor else unobtainedHighlightColor
        }
        return true
    }

    @HandleEvent
    fun onInventoryClose() {
        inCakeInventory = false
        knownCakesInCurrentInventory = listOf()
        inAuctionHouse = false
        slotHighlightCache = mapOf()
        searchingForCakes = false
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed() {
        if (!config.enabled) return
        val sbTimeNow = SkyBlockTime.now()
        if (currentYear == sbTimeNow.year) return
        if (sbTimeNow.month == 12 && sbTimeNow.day >= 29) {
            currentYear = sbTimeNow.year
            recalculateMissingCakes()
        } else currentYear = sbTimeNow.year - 1
    }

    private fun checkInventoryCakes() {
        if (timeOpenedCakeInventory.passedSince() < 500.milliseconds) return
        val currentYears = InventoryUtils.getItemsInOpenChest().mapNotNull { item ->
            cakeNamePattern.matchGroup(item.stack.displayName, "year")?.toInt()
        }

        val addedYears = currentYears.filter { it !in knownCakesInCurrentInventory }
        val removedYears = knownCakesInCurrentInventory.filter { it !in currentYears }

        addedYears.forEach(::addCake)
        removedYears.forEach(::removeCake)

        if (addedYears.isNotEmpty() || removedYears.isNotEmpty()) {
            knownCakesInCurrentInventory = currentYears.toMutableList()
        }
    }

    private fun recalculateMissingCakes() {
        val storage = storage ?: return
        storage.missingCakes = ((1..currentYear).toSet() - storage.ownedCakes).toMutableSet()
    }

    private fun getCakePrice(year: Int): Double {
        return cakePriceCache.getOrPut(year) {
            val cakeItem = "NEW_YEAR_CAKE+$year".toInternalName()
            cakeItem.getPrice()
        }
    }

    private fun getCakePriceString(year: Int): String {
        return getCakePrice(year).takeIf { it > 0 }?.let {
            "§6${it.addSeparators()}"
        } ?: "§7Unknown (no auctions)"
    }

    private data class CakeRange(var start: Int, var end: Int = 0) {
        // When end is 0 or equal to start, we consider the range singular.
        private val isSingular = (start == end || end == 0)

        // Create the list of valid values in the intended order.
        // If start < end, it’s an ascending range; otherwise, descending.
        private val values: List<Int> = if (start < end) (start..end).toList() else (start downTo end).toList()

        var selectedSingular: Int = getSelectedCake(start, end)
            ?.takeIf { it in values }
            ?: start

        /**
         * Changes the currently selected number by a given delta.
         *
         * The change moves along the list of valid values (which will be ascending or descending
         * depending on the input order) and wraps around at the ends.
         *
         * @param delta The amount to change by (e.g. +1 for "next", -1 for "previous").
         */
        private fun changeSelectedSingular(delta: Int) {
            // If the range is singular, there's nothing to change.
            if (isSingular) return

            // Find the current index in the ordered list.
            val currentIndex = values.indexOf(selectedSingular)
            // Compute the new index and wrap around (the mod operator might yield negative values,
            // so we adjust accordingly).
            val newIndex = ((currentIndex + delta) % values.size + values.size) % values.size
            selectedSingular = values[newIndex]

            searchOverrideCache[start to end] = selectedSingular
            invalidateCakeCache()
            SoundUtils.playClickSound()
        }

        fun getRenderable(displayType: DisplayType): Renderable {
            val colorCode: String = if (displayType == DisplayType.OWNED_CAKES) "§a" else "§c"
            val displayString =
                if (isSingular) "§fYear $colorCode$start"
                else "§fYears $colorCode$start§f-$colorCode$end"
            var renderable: Renderable = RenderableString(displayString)
            if (displayType == DisplayType.MISSING_CAKES && config.priceOnHover) {
                renderable = Renderable.clickable(
                    renderable,
                    tips = getPriceHoverTooltip(displayType, colorCode),
                    onAnyClick = mapOf(
                        KEY_LEFT to { changeSelectedSingular(-1) },
                        KEY_UP to { changeSelectedSingular(-1) },
                        KEY_RIGHT to { changeSelectedSingular(1) },
                        KEY_DOWN to { changeSelectedSingular(1) },
                        LEFT_MOUSE to { HypixelCommands.auctionSearch("New Year Cake (Year $selectedSingular)") },
                    ),
                )
            }
            return renderable
        }

        private fun Int.getYearString(colorCode: String): String {
            val baseString = "${colorCode}Year $this§7: ${getCakePriceString(this)}"
            val preAmbleString = if (this == selectedSingular && !isSingular) "§e▶ " else "  "
            return "$preAmbleString$baseString"
        }

        fun getPriceHoverTooltip(displayType: DisplayType, colorCode: String): List<String> {
            return if (isSingular) {
                listOf(
                    "${colorCode}Year $start§7: ${getCakePriceString(start)}",
                    "§eClick to search auction house",
                )
            } else buildList {
                val largerNumber = if (start > end) start else end
                val smallerNumber = if (start < end) start else end
                val allYears = (smallerNumber..largerNumber).toList()
                val totalCount = allYears.size

                val subList = if (totalCount <= 5) allYears
                else {
                    val selectedIndex = allYears.indexOf(selectedSingular)
                    val windowStart = when {
                        selectedIndex < 2 -> 0
                        selectedIndex > totalCount - 3 -> totalCount - 5
                        else -> selectedIndex - 2
                    }
                    allYears.subList(windowStart, windowStart + 5)
                }

                val isWindowLow = subList.first() != allYears.first()
                val isWindowHigh = subList.last() != allYears.last()

                if (isWindowLow) add("  §7§oMore above...")
                subList.forEach { add(it.getYearString(colorCode)) }
                if (isWindowHigh) add("  §7§oMore below...")

                val anyIndeterminate = subList.any { getCakePrice(it) == 0.0 }
                val addendum = if (anyIndeterminate) "§7*" else ""
                val priceSum = allYears.sumOf(::getCakePrice)
                val totalString = "§aTotal§7: §6${priceSum.addSeparators()}$addendum"

                add("")
                if (priceSum != 0.0) add(totalString)
                else add("§cNo auctions found")
                if (displayType == DisplayType.MISSING_CAKES) {
                    add("§eClick to search auction house!")
                    add("§8Use arrow keys to change year!")
                }
            }
        }
    }

    private fun MutableList<Renderable>.addDisplayTypeToggle() = addRenderableButton<CakeTrackerDisplayType>(
        label = "Display",
        current = config.displayType.get(),
        onChange = { config.displayType.set(it) },
        getName = { it.toString() },
    )

    private fun MutableList<Renderable>.addOrderTypeToggle() = addRenderableButton<CakeTrackerDisplayOrderType>(
        label = "Order",
        current = config.displayOrderType.get(),
        onChange = { config.displayOrderType.set(it) },
        getName = { it.toString() },
    )

    private fun drawDisplay(data: CakeData): List<Renderable> = buildList {
        val dataHash = data.hashCode()
        if (dataHash != lastKnownCakeDataHash) {
            cakeRenderables = buildCakeRenderables(data)
            lastKnownCakeDataHash = dataHash
        }

        addAll(cakeRenderables)
    }

    private fun getHeaderTips(data: CakeData) = buildList {
        val unknownOwned = data.ownedCakes.count { getCakePrice(it) == 0.0 }
        val unknownMissing = data.missingCakes.count { getCakePrice(it) == 0.0 }

        add("§aHave§7: §a${data.ownedCakes.size}")
        add("§6Total value§7: §6${data.ownedCakes.sumOf(::getCakePrice).addSeparators()}")
        if (unknownOwned > 0) add("  §7§o* $unknownOwned unknown prices")
        add("")
        add("§cMissing§7: §c${data.missingCakes.size}")
        add("§6Total cost§7: §6${data.missingCakes.sumOf(::getCakePrice).addSeparators()}")
        if (unknownMissing > 0) add("  §7§o* $unknownMissing unknown prices")
        add("")
        add("§bPercent owned§7: §a${"%.2f".format(data.ownedCakes.size * 100.0 / currentYear)}%")
    }

    private fun buildCakeRenderables(data: CakeData) = buildList {
        add(Renderable.hoverTips("§c§lNew §f§lYear §c§lCake §f§lTracker", getHeaderTips(data)))
        addDisplayTypeToggle()
        addOrderTypeToggle()

        val displayType = config.displayType.get() ?: return@buildList
        val displayOrderType = config.displayOrderType.get() ?: return@buildList

        val cakeList = when (displayType) {
            DisplayType.OWNED_CAKES -> data.ownedCakes
            DisplayType.MISSING_CAKES -> data.missingCakes
        }

        if (cakeList.isEmpty()) {
            val colorCode = if (displayType == DisplayType.OWNED_CAKES) "§c" else "§a"
            val verbiage = if (displayType == DisplayType.OWNED_CAKES) "missing" else "owned"
            add(RenderableString("$colorCode§lAll cakes $verbiage!"))
        } else add(
            Renderable.scrollList(
                getCakeRanges(cakeList, displayType, displayOrderType),
                height = maxTrackerHeight.toInt() + 2, // +2 to account for tips
                velocity = 20.0,
                showScrollableTipsInList = true,
                scrollValue = cakeScrollValue,
            ),
        )
    }

    private fun getCakeRanges(
        cakeList: Set<Int>,
        displayType: DisplayType,
        orderType: DisplayOrder,
    ): List<Renderable> = buildList {
        val sortedCakes = when (orderType) {
            DisplayOrder.OLDEST_FIRST -> cakeList.sorted()
            DisplayOrder.NEWEST_FIRST -> cakeList.sortedDescending()
        }

        var start = sortedCakes.first()
        var end = start

        for (year in sortedCakes.drop(1)) { // Skip the first item to prevent duplicate addition
            val oldestFirstAtEnd = orderType == DisplayOrder.OLDEST_FIRST && year == end + 1
            val newestFirstAtEnd = orderType == DisplayOrder.NEWEST_FIRST && year == end - 1

            if (oldestFirstAtEnd || newestFirstAtEnd) end = year
            else {
                val range = if (start != end) CakeRange(start, end) else CakeRange(start)
                add(range.getRenderable(displayType))
                start = year
                end = start
            }
        }

        val lastRange =
            if (start != end) CakeRange(start, end)
            else CakeRange(start)

        add(lastRange.getRenderable(displayType))
    }
}
