package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.config.features.misc.tracker.TopLevelTrackerConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.generic.ItemTrackerSettings
import at.hannibal2.skyhanni.config.features.misc.tracker.generic.ItemTrackerSettings.ItemTrackerConfig.TextPart
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.TrackerManager
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValue
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ClipboardUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemPriceSource
import at.hannibal2.skyhanni.utils.ItemPriceUtils.formatCoin
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceName
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.readableInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemNameCompact
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.SKYBLOCK_COIN
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.pluralize
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sortedDesc
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import at.hannibal2.skyhanni.utils.inPartialHours
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addButton
import at.hannibal2.skyhanni.utils.renderables.ScrollValue
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.empty
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.tracker.data.BucketedItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.data.ItemTrackerData
import net.minecraft.ChatFormatting
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

abstract class SkyHanniItemTracker<Data : ItemTrackerData<*>>(name: String) : SkyHanniTracker<Data>(name) {

    abstract override val config: TopLevelTrackerConfig

    // Chains through the base useUniversalConfig check, then casts. The cast is safe because
    // this class constrains config to TopLevelTrackerConfig<ItemTrackerSettings>, so both the
    // per-tracker config and universalTracker (which extends ItemTrackerSettings) satisfy it.
    override val trackerConfig: ItemTrackerSettings get() = super.trackerConfig as ItemTrackerSettings

    override fun shouldRender(): Boolean {
        if (trackerConfig.itemTracker.hideInEstimatedItemValue && EstimatedItemValue.isCurrentlyShowing()) return false
        if (!InventoryUtils.inAnyInventory() && trackerConfig.itemTracker.hideOutsideInventory) return false
        return true
    }

    private val scrollValue = ScrollValue()

    open fun addCoins(amount: Int, command: Boolean) = modify {
        it.addItem(SKYBLOCK_COIN, amount, command)
    }

    open fun addItem(internalName: NeuInternalName, amount: Int, command: Boolean, message: Boolean = true) {
        modify {
            it.addItem(internalName, amount, command)
        }
        getSharedTracker()?.let { sharedData ->
            val isHidden = sharedData.get(DisplayMode.TOTAL).items[internalName]?.hidden
            if (isHidden != null) sharedData.modify { it.items[internalName]?.hidden = isHidden }
        }

        if (command) logCommandAdd(internalName, amount)
        handlePossibleRareDrop(internalName, amount, message)
    }

    open fun ItemAddEvent.addItemFromEvent() = modify { data ->
        data.addItem(internalName, amount, command = (source == ItemAddManager.Source.COMMAND))
        logCompletedAddEvent()
    }

    fun logCommandAdd(internalName: NeuInternalName, amount: Int) {
        val action = if (amount > 0) "added to" else "removed from"
        ChatUtils.chat("Manually $action $name: ${internalName.getPriceName(amount.absoluteValue)}")
    }

    fun ItemAddEvent.logCompletedAddEvent() {
        if (source != ItemAddManager.Source.COMMAND) return
        TrackerManager.commandEditTrackerSuccess = true
        logCommandAdd(internalName, amount)
    }

    fun handlePossibleRareDrop(
        internalName: NeuInternalName,
        amount: Int,
        message: Boolean = true,
    ) = with(trackerConfig.itemTracker) {
        val (itemName, price) = SlayerApi.getItemNameAndPrice(internalName, amount)
        if (warnings.chat && price >= warnings.minimumChat && message) {
            componentBuilder {
                appendWithColor("+Tracker Drop", ChatFormatting.GREEN)
                appendWithColor(": ", ChatFormatting.GRAY)
                append("§r$itemName")
            }.let(ChatUtils::chat)
        }
        if (warnings.title && price >= warnings.minimumTitle) {
            TitleManager.sendTitle("§a+ $itemName", weight = price)
        }
    }

    private fun NeuInternalName.getCleanName(
        items: Map<NeuInternalName, ItemTrackerData.TrackedItem>,
        getCoinName: (ItemTrackerData.TrackedItem) -> String,
    ): String {
        val item = items[this] ?: error("Item not found for $this")
        return if (this == SKYBLOCK_COIN) getCoinName.invoke(item) else this.repoItemNameCompact
    }

    /**
     * Renders the item list for this tracker and returns the total profit value.
     *
     * [context] provides overridable accessors and action callbacks. Use [DrawItemsContext.default]
     * for standard trackers; bucketed trackers supply their own instance to route reads and
     * mutations through the selected bucket.
     */
    open fun drawItems(
        data: Data,
        filter: (NeuInternalName) -> Boolean,
        lists: MutableList<Searchable>,
        context: DrawItemsContext = DrawItemsContext.default(data, this),
    ): Double {
        var profit = 0.0
        val items = mutableMapOf<NeuInternalName, Long>()
        val dataItems = context.itemsAccessor.invoke()
        val itemTrackerConfig = trackerConfig.itemTracker
        for ((internalName, itemProfit) in dataItems) {
            if (!filter(internalName)) continue

            val amount = itemProfit.totalAmount
            val pricePer = if (internalName == SKYBLOCK_COIN) 1.0 else data.getCustomPricePer(internalName, this)
            val price = (pricePer * amount).toLong()
            val hidden = itemProfit.hidden

            if (isInventoryOpen() || !hidden) {
                items[internalName] = price
            }
            if (!hidden || !itemTrackerConfig.excludeHiddenItemsInPrice) {
                profit += price
            }
        }

        val table = mutableMapOf<List<Renderable>, String>()
        for ((internalName, price) in items.sortedDesc()) {
            val itemProfit = dataItems[internalName] ?: error("Item not found for $internalName")

            val amount = itemProfit.totalAmount
            val displayAmount = if (internalName == SKYBLOCK_COIN) itemProfit.timesGained else amount

            val cleanName = internalName.getCleanName(dataItems, context.getCoinName)

            val hidden = itemProfit.hidden
            val priceFormat = price.formatCoin(gray = hidden)
            val newDrop = itemProfit.lastTimeUpdated.passedSince() < 10.seconds && itemTrackerConfig.showRecentDrops
            val numberColor = if (newDrop) "§a§l" else "§7"

            val formattedName = cleanName.removeColor(keepFormatting = true).replace("§r", "")
            val displayName = if (hidden) "§8§m$formattedName" else cleanName

            val loreText = context.getLoreList.invoke(internalName, itemProfit)
            val lore: List<String> = buildLore(loreText, hidden, newDrop, internalName)

            // TODO add row abstraction to api, with common click+hover behaviour
            fun string(string: String): Renderable = if (isInventoryOpen()) Renderable.clickable(
                string,
                tips = lore,
                onLeftClick = {
                    if (KeyboardManager.isModifierKeyDown()) context.itemRemover.invoke(internalName, cleanName)
                    else context.itemHider.invoke(internalName, hidden)
                    // TODO remove unnecessary update call, as both invokes above call the modify fun. in modify there is also a update call
                    update()
                },
            ) else Renderable.text(string)

            val row = mutableMapOf<TextPart, Renderable>()
            row[TextPart.NAME] = string(" $displayName")

            row[TextPart.ICON] = if (internalName == SKYBLOCK_COIN) {
                Renderable.item(ItemUtils.getCoinItemStack(amount))
            } else {
                Renderable.item(internalName)
            }

            row[TextPart.TOTAL_PRICE] = string(" $priceFormat")
            row[TextPart.AMOUNT] = string(" $numberColor${displayAmount.addSeparators()}x")

            val line = itemTrackerConfig.textOrder.get().mapNotNull { row[it] }
            table[line] = cleanName
        }

        val scrollValue = (data as? BucketedItemTrackerData<*, *>)?.selectedScrollValue ?: scrollValue
        Renderable.searchableScrollable(
            table,
            key = 99,
            lines = min(items.size, itemTrackerConfig.itemsShown.get()),
            velocity = 5.0,
            textInput = textInput,
            scrollValue = scrollValue,
            asTable = itemTrackerConfig.showTable.get(),
            showScrollableTipsInList = isInventoryOpen(),
        )?.let {
            lists.add(it.toSearchable())
        }

        return profit
    }

    private fun buildLore(
        loreFormat: List<String>,
        hidden: Boolean,
        newDrop: Boolean,
        internalName: NeuInternalName,
    ) = buildList {
        add(internalName.repoItemName)
        add("")
        addAll(loreFormat)
        add("")
        if (newDrop) {
            add("§aYou obtained this item recently.")
            add("")
        }
        add("§eClick to " + (if (hidden) "show" else "hide") + "!")
        add("§eControl + Click to remove this item!")

        add("")
        add("§7Use §e/shedittracker ${internalName.readableInternalName} <amount>")
        add("§7to edit the number.")
        add("§7Use negative numbers to remove items.")

        if (SkyBlockUtils.debug) {
            add("")
            add("§7$internalName")
        }
    }

    private val copyOnClickConfig by lazy { CoroutineSettings("$name copy on click") }

    private fun copyOnClick(line: String, fullTipsLine: String, type: String) = copyOnClickConfig.launch {
        val copied = ClipboardUtils.copyToClipboardAsync(
            if (KeyboardManager.isShiftKeyDown()) fullTipsLine
            else line,
        ).await() ?: false
        if (copied) ChatUtils.chat("§eCopied $name $type to clipboard!")
        else ChatUtils.chat("§cFailed to copy $name $type to clipboard!")
    }

    fun addTotalProfit(
        profit: Double,
        totalAmount: Long,
        action: String,
        duration: Duration,
        actionPluralized: String = "",
        actionShorten: Boolean = true,
    ): List<Searchable> {
        val profitFormat = profit.toLong().addSeparators()
        val profitPrefix = if (profit < 0) "§c" else "§6"

        val profitTips = buildList {
            if (totalAmount > 0) {
                val profitPerCatch = profit / totalAmount
                add("§7Profit per $action: $profitPrefix${profitPerCatch.shortFormat()}")
            }

            if (duration > 0.seconds) {
                val profitPerHour = profit / duration.inPartialHours
                add("§7Profit per hour: $profitPrefix${profitPerHour.shortFormat()}")
            }

            if (totalAmount > 0 && duration > 0.seconds && actionPluralized != "") {
                val amountPerHour = totalAmount / duration.inPartialHours
                val amount = if (actionShorten) amountPerHour.shortFormat() else amountPerHour
                add("§7$actionPluralized per hour: §e$amount")
            }
        }

        val tips: List<String> = buildList {
            addAll(profitTips)
            addAll(
                listOf(
                    "",
                    "§eClick to copy line!",
                    "§eShift Click to include stats in this tooltip!",
                ),
            )
        }

        val coinFormat = "coin".pluralize(profit.toInt())
        val text = "§e${getDisplayMode()} Profit: $profitPrefix$profitFormat $coinFormat"

        val profitRenderable = Renderable.clickable(
            text,
            tips = tips,
            onLeftClick = {
                val line = "$name: ${text.removeColor()}"
                val tipStats = profitTips.take(2)
                val fullTipsLine = line + "\n " + tipStats.joinToString(" \n") { it.removeColor() }
                copyOnClick(line, fullTipsLine, "profit")
            },
        )
        val profitPerHourRenderable =
            if (shouldShowProfitPerHour()) profitPerHourRenderable(profit, duration) else Renderable.empty()
        return listOf(profitRenderable.toSearchable(), profitPerHourRenderable.toSearchable())
    }

    private fun shouldShowProfitPerHour() =
        trackerConfig.itemTracker.profitPerHour.get() && !(getDisplayMode() == DisplayMode.TOTAL && trackerConfig.onlyShowSession.get())

    private fun profitPerHourRenderable(profit: Double, duration: Duration): Renderable {
        if (duration == 0.seconds) return Renderable.empty()
        val profitPerHour = profit / duration.inPartialHours
        val profitPerHourFormat = profitPerHour.roundTo(0).addSeparators()
        val coinFormat = "coin".pluralize(profitPerHour.toInt())
        val profitPrefix = if (profitPerHour < 0) "§c" else "§6"
        val text = "§eProfit Per Hour: $profitPrefix$profitPerHourFormat $coinFormat"

        val tips = listOf(
            "§7Uptime: §b${duration.format()}",
            "",
            "§eClick to copy line!",
            "§eShift Click to include stats in this tooltip!",
        )
        return Renderable.clickable(
            text,
            tips = tips,
            onLeftClick = {
                val line = "$name: ${text.removeColor()}"
                val tipStats = tips[0]
                val fullTipsLine = "$line\n${tipStats.removeColor()}"
                copyOnClick(line, fullTipsLine, "profit per hour")
            },
        )
    }

    fun addPriceFromButton(lists: MutableList<Searchable>) {
        if (!isInventoryOpen()) return
        lists.addButton(
            label = "Price Source",
            current = trackerConfig.priceSource,
            getName = { it.sellName },
            onChange = {
                trackerConfig.priceSource = it
                update()
            },
            universe = ItemPriceSource.entries,
        )
    }
}
