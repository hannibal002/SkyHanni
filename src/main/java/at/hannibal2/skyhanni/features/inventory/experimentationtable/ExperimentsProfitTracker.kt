package at.hannibal2.skyhanni.features.inventory.experimentationtable

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.ExperimentationTableApi
import at.hannibal2.skyhanni.api.ExperimentationTableApi.experienceBottlePattern
import at.hannibal2.skyhanni.api.ExperimentationTableApi.experimentRenewPattern
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.experiments.TableTaskCompletedEvent
import at.hannibal2.skyhanni.events.experiments.TableXPBottleUsedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemPriceSource
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getNpcPriceOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.pluralize
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.enumMapOf
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import com.google.gson.annotations.Expose
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ExperimentsProfitTracker {
    private val config get() = SkyHanniMod.feature.inventory.experimentationTable.experimentsProfitTracker
    private val tracker = SkyHanniItemTracker(
        "Experiments Profit Tracker",
        { Data() },
        { it.experimentation.experimentsProfitTracker },
    ) { drawDisplay(it) }

    // Warn once per session about tracking XP bottle usage
    private var warnedAboutTracking = false

    class Data : ItemTrackerData() {
        override fun resetItems() {
            experimentsDone = 0L
            xpGained = 0L
            bitCost = 0L
            startCost = 0L
            timeWasted = enumMapOf()
        }

        override fun getDescription(timesGained: Long): List<String> {
            val percentage = timesGained.toDouble() / experimentsDone
            val dropRate = percentage.coerceAtMost(1.0).formatPercentage()
            return listOf(
                "§7Dropped §e${timesGained.addSeparators()} §7times.",
                "§7Your drop rate: §c$dropRate.",
            )
        }

        override fun getCoinName(item: TrackedItem) = ""

        override fun getCoinDescription(item: TrackedItem) = listOf<String>()

        @Expose
        var experimentsDone = 0L

        @Expose
        var xpGained = 0L

        @Expose
        var bitCost = 0L

        @Expose
        var startCost = 0L

        @Expose
        var timeWasted: MutableMap<ExperimentationTableApi.ExperimentationTaskType, Duration> = enumMapOf()
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onItemAdd(event: ItemAddEvent) {
        if (!isEnabled() || !config.enabled || event.source != ItemAddManager.Source.COMMAND) return
        tracker.addItem(event.internalName, event.amount, command = true)
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onChat(event: SkyHanniChatEvent) {
        if (!isEnabled()) return
        experimentRenewPattern.matchMatcher(event.message.removeColor()) {
            val increments = mapOf(1 to 150, 2 to 300, 3 to 500)
            tracker.modify {
                it.bitCost += increments.getValue(group("current").toInt())
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onTableTaskCompleted(event: TableTaskCompletedEvent) {
        tracker.modify {
            if (event.type == ExperimentationTableApi.ExperimentationTaskType.SUPERPAIRS) {
                it.experimentsDone++
            }
            it.xpGained += event.enchantingXpGained ?: 0L
        }
        event.loot.forEach { (item, count) ->
            tracker.addItem(item, count, command = false)
        }
    }

    private val allowedSlots = listOf(11, 12, 14, 15)
    private val bottlesInventory = InventoryDetector { name -> name == "Bottles of Enchanting" }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled() || !bottlesInventory.isInside() || !allowedSlots.contains(event.slotId)) return
        val internalName = event.slot?.stack?.getInternalNameOrNull()?.takeIf {
            experienceBottlePattern.matches(it.asString())
        } ?: return

        // If you click the button with a bottle of that type already in your inventory,
        // hypixel uses that one instead of buying one from the bazaar.
        val hasApplicableBottle = InventoryUtils.getItemsInOwnInventory().any {
            it.getInternalNameOrNull() == internalName
        }
        if (hasApplicableBottle) return

        tracker.modify {
            it.startCost -= calculateBottlePrice(internalName)
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onTableXpBottleUsed(event: TableXPBottleUsedEvent) {
        if (!isEnabled() || !config.trackUsedBottles) return
        val bottlePrice = calculateBottlePrice(event.internalName)
        tracker.modify {
            it.startCost -= (bottlePrice * event.amount)
        }
        if (warnedAboutTracking || !config.bottleWarnings) return
        warnedAboutTracking = true
        ChatUtils.clickToActionOrDisable(
            event.internalName.formatWarningString(event.amount),
            config::trackUsedBottles,
            actionName = "undo",
            action = {
                tracker.modify {
                    it.startCost += (bottlePrice * event.amount)
                    val bottleFormat = "bottle".pluralize(event.amount)
                    ChatUtils.chat("Un-did the tracking of ${event.amount} $bottleFormat!")
                }
            },
            oneTimeClick = true,
        )
    }

    private var lastAddedTimeWasted: SimpleTimeMark = SimpleTimeMark.farPast()

    @HandleEvent(SecondPassedEvent::class)
    fun checkAddTimeWasted() {
        if (ExperimentationTableApi.expOverInventoryPattern.matches(InventoryUtils.openInventoryName())) return
        if (!ExperimentationTableApi.inTable || !config.trackTimeSpent) {
            lastAddedTimeWasted = SimpleTimeMark.farPast()
            return
        }
        val currentType = ExperimentationTableApi.currentExperimentType ?: return
        lastAddedTimeWasted.takeIfInitialized()?.let { lastAdded ->
            tracker.modify {
                it.timeWasted.addOrPut(currentType, lastAdded.passedSince())
            }
        }
        lastAddedTimeWasted = SimpleTimeMark.now()
    }

    private fun NeuInternalName.formatWarningString(amount: Int) = buildString {
        val displayName = getItemStackOrNull()?.displayName ?: "XP Bottle"
        val amountFormat = "§8${amount}x ".takeIf { amount > 1 }.orEmpty()
        appendLine("§aExperiments Tracker§7:")
        appendLine("§eAutomatically tracked usage of $amountFormat$displayName §ewhile near the Experimentation Table§7.")
        appendLine("§7This warning can also be disabled in the config.")
    }

    private fun calculateBottlePrice(internalName: NeuInternalName): Int {
        val price = SkyHanniTracker.getPricePer(internalName)
        val npcPrice = internalName.getNpcPriceOrNull() ?: 0.0
        return npcPrice.coerceAtLeast(price).toInt()
    }

    private fun drawDisplay(data: Data): List<Searchable> = buildList {
        addSearchString("§e§lExperiments Profit Tracker")
        val startCost = when (SkyHanniMod.feature.misc.tracker.priceSource) {
            ItemPriceSource.NPC_SELL -> 0
            else -> data.startCost
        }
        val profit = tracker.drawItems(data, { true }, this) + startCost
        addSearchString("§eExperiments Done: §a${data.experimentsDone.addSeparators()}")

        if (config.trackTimeSpent) {
            val timeFormat = data.timeWasted.values.sumOf { it.inWholeSeconds }.seconds.format()
            addSearchString("§eTime Spent: §b$timeFormat")
        }

        val startCostFormat = startCost.absoluteValue
        val bitCostFormat = data.bitCost
        add(
            Renderable.hoverTips(
                "§eTotal Cost: §c-${startCostFormat.shortFormat()}§e/§b-${bitCostFormat.shortFormat()}",
                listOf(
                    "§7You paid §c${startCostFormat.addSeparators()} §7coins and",
                    "§b${bitCostFormat.addSeparators()} §7bits for starting",
                    "§7experiments.",
                ),
            ).toSearchable(),
        )
        add(tracker.addTotalProfit(profit, data.experimentsDone, "experiment"))

        val enchantingXpGained = data.xpGained
        add(
            Renderable.hoverTips(
                "§eTotal Enchanting Exp: §b${enchantingXpGained.shortFormat()}",
                listOf(
                    "§7You gained §b${enchantingXpGained.addSeparators()} §7Enchanting Exp",
                    "§7from experiments.",
                ),
            ).toSearchable(),
        )

        tracker.addPriceFromButton(this)
    }

    init {
        tracker.initRenderer(
            { config.position },
            inventory = ExperimentationTableApi.experimentationTableInventory,
            onlyOnIsland = IslandType.PRIVATE_ISLAND,
        ) { isEnabled() }
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onIslandChange(event: IslandChangeEvent) {
        tracker.firstUpdate()
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shresetexperimentsprofittracker") {
            description = "Resets the Experiments Profit Tracker"
            category = CommandCategory.USERS_RESET
            callback { tracker.resetCommand() }
        }
    }

    private fun isEnabled() = config.enabled && ExperimentationTableApi.inDistanceToTable(5.0)
}
