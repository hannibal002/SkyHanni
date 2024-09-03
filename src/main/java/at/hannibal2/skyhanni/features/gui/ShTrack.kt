package at.hannibal2.skyhanni.features.gui

import at.hannibal2.skyhanni.api.CollectionAPI
import at.hannibal2.skyhanni.config.commands.CommandArgument
import at.hannibal2.skyhanni.config.commands.CommandContextAwareObject
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.data.SackAPI.getAmountInSacks
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils.getAmountInInventory
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUCalculator
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.NEUItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ShTrack {

    val arguments = listOf<CommandArgument<ContextObject>>(
        CommandArgument("-i") { _, c -> c.state = ContextObject.StateType.ITEM; 0 },
        CommandArgument(defaultPosition = 0) { a, c -> numberCalculate(a, c) { context, number -> context.targetAmount = number } },
        CommandArgument(defaultPosition = 1, validity = ::validIfItemState) { a, c ->
            val r = itemCheck(a, c)
            r.second?.let { c.item = it }
            r.first
        },
        CommandArgument("-c", defaultPosition = -2) { a, c -> numberCalculate(a, c) { context, number -> context.currentAmount = number } },
        CommandArgument("-s", validity = ::validIfItemState) { _, c -> c.currentFetch = ContextObject.CurrentFetch.SACKS; 0 },
        CommandArgument("-v", validity = ::validIfItemState) { _, c -> c.currentFetch = ContextObject.CurrentFetch.INVENTORY; 0 },
        CommandArgument("-cc", validity = ::validIfItemState) { _, c -> c.currentFetch = ContextObject.CurrentFetch.COLLECTION; 0 },
        CommandArgument("-d") { _, c -> c.allowDupe = true; 0 },
        CommandArgument("-k") { _, c -> c.autoDelete = false; 0 },
        CommandArgument("-n") { _, c -> c.notify = true; 0 },
    )

    private fun validIfItemState(context: ContextObject) = context.state == ContextObject.StateType.ITEM

    fun <T : CommandContextAwareObject> numberCalculate(args: Iterable<String>, context: T, use: (T, Long) -> Unit): Int {
        NEUCalculator.calculateOrNull(args.firstOrNull())?.toLong()?.let { use(context, it) } ?: {
            context.errorMessage = "Unkown number/calculation: '${args.firstOrNull()}'"
        }
        return args.firstOrNull()?.let { 1 } ?: 0
    }

    fun itemCheck(args: Iterable<String>, context: CommandContextAwareObject): Pair<Int, NEUInternalName?> {
        if (args.count() == 0) {
            context.errorMessage = "No item specified"
            return 0 to null
        }
        val first = args.first()

        val namePattern = "^name:".toRegex()
        val internalPattern = "^internal:".toRegex()

        val expectInternalElseName: Boolean? = if (namePattern.matches(first)) {
            false
        } else if (internalPattern.matches(first)) {
            true
        } else {
            null
        }

        val grabbed = args.takeWhile { "[a-zA-Z:\\-_\"';]+([:-;]\\d+)?".toPattern().matches(it) }

        val collected = grabbed.joinToString(" ").replace("[\"']".toRegex(), "")

        val item = when (expectInternalElseName) {
            true -> collected.replace(internalPattern, "").replace(" ", "_").asInternalName()
            false -> NEUInternalName.fromItemNameOrNull(collected.replace(namePattern, "").replace("_", " "))
            null -> {
                val fromItemName = NEUInternalName.fromItemNameOrNull(collected.replace("_", " "))
                if (fromItemName?.getItemStackOrNull() != null) {
                    fromItemName
                } else {
                    val internalName = collected.replace(" ", "_").asInternalName()
                    if (internalName.getItemStackOrNull() != null) {
                        internalName
                    } else {
                        null
                    }
                }
            }
        }

        if (item?.getItemStackOrNull() == null) {
            context.errorMessage = "Could not find a valid item for: '$collected'"
        }

        return grabbed.size to item
    }

    class ContextObject : CommandContextAwareObject {

        var allowDupe = false
        var autoDelete = true
        var notify = false

        var state: StateType? = null
            set(value) {
                if (value == null) {
                    field = null
                }
                if (field == null) {
                    field = value
                } else {
                    errorMessage = "Illegal double state assigment from '$field' to '$value'"
                }
            }

        var item: NEUInternalName? = null
        var targetAmount: Long? = null
        var currentAmount: Long? = null
            set(value) {
                if (value == null) {
                    field = null
                }
                if (field == null && currentFetch == null) {
                    field = value
                } else {
                    errorMessage = if (field != null) {
                        "Illegal double current amount from '$field' to '$value'"
                    } else {
                        "Illegal double current amount from '$currentFetch' to '$value'"
                    }

                }
            }

        var currentFetch: CurrentFetch? = null
            set(value) {
                if (value == null) {
                    field = null
                }
                if (field == null && currentAmount == null) {
                    field = value
                } else {
                    errorMessage = if (field != null) {
                        "Illegal double current amount from '$field' to '$value'"
                    } else {
                        "Illegal double current amount from '$currentAmount' to '$value'"
                    }

                }
            }

        override var errorMessage: String? = null

        override fun post() {
            val result: TrackingElement
            when (state) {
                StateType.ITEM -> {
                    val item = item ?: run {
                        errorMessage = "No item specified"
                        return
                    }
                    val current: Long = currentAmount ?: when (currentFetch) {
                        CurrentFetch.INVENTORY -> item.getAmountInInventory().toLong()
                        CurrentFetch.SACKS -> item.getAmountInInventory().toLong() + item.getAmountInSacks().toLong()
                        CurrentFetch.COLLECTION -> CollectionAPI.getCollectionCounter(item) ?: run {
                            errorMessage = "Collection amount is unknown"
                            0L
                        }

                        else -> 0L
                    }
                    result = ItemTrackingElement(item, current, targetAmount, currentFetch != CurrentFetch.INVENTORY)
                }

                else -> {
                    errorMessage = "Unknown Stat"
                    return
                }
            }
            result.shouldNotify = notify
            result.shouldAutoDelete = autoDelete
            if (!allowDupe) {
                val index = tracker.indexOfFirst { result.similarElement(it) }
                if (index != -1) {
                    tracker[index] = result
                }
            }
            tracker.add(result)
        }

        enum class StateType {
            ITEM
        }

        enum class CurrentFetch {
            INVENTORY,
            COLLECTION,
            SACKS,

        }
    }

    private val tracker: MutableList<TrackingElement> = object : ArrayList<TrackingElement>() {

        override fun clear() {
            forEach {
                it.atRemove()
            }
            super.clear()
            updateDisplay()
        }

        override fun addAll(elements: Collection<TrackingElement>): Boolean {
            elements.forEach { it.atAdd() }
            val r = super.addAll(elements)
            updateDisplay()
            return r
        }

        override fun removeAt(index: Int): TrackingElement {
            this[index].atRemove()
            val r = super.removeAt(index)
            updateDisplay()
            return r
        }

        override fun set(index: Int, element: TrackingElement): TrackingElement {
            this.getOrNull(index)?.atRemove()
            element.atAdd()
            val r = super.set(index, element)
            updateDisplay()
            return r
        }

        override fun add(element: TrackingElement): Boolean {
            element.atAdd()
            val r = super.add(element)
            updateDisplay()
            return r
        }

        override fun add(index: Int, element: TrackingElement) {
            element.atAdd()
            val r = super.add(index, element)
            updateDisplay()
            return r
        }

        override fun addAll(index: Int, elements: Collection<TrackingElement>): Boolean {
            elements.forEach { it.atAdd() }
            val r = super.addAll(index, elements)
            updateDisplay()
            return r
        }

        override fun removeRange(fromIndex: Int, toIndex: Int) {
            (fromIndex..<toIndex).forEach { this[it].atRemove() }
            super.removeRange(fromIndex, toIndex)
            updateDisplay()
        }
    }

    private val itemTrackers: MutableMap<NEUInternalName, MutableList<ItemTrackingElement>> = mutableMapOf()

    private val position = Position(20, 20)

    private var display: Renderable = Renderable.placeholder(0, 0)

    @SubscribeEvent
    fun onGuiRenderGuiOverlayRender(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (scheduledUpdate) {
            display = Renderable.table(tracker.map { it.line })
        }
        position.renderRenderable(display, posLabel = "Tracker")
    }

    @SubscribeEvent
    fun onItemAdd(event: ItemAddEvent) {
        val trackers = itemTrackers[event.internalName] ?: return
        if (event.source == ItemAddManager.Source.SACKS) {
            for (tracker in trackers) {
                if (!tracker.includeSack) continue
                tracker.update(event.amount)
            }
        } else {
            for (tracker in trackers) {
                tracker.update(event.amount)
            }
        }

    }

    private var scheduledUpdate = false

    fun updateDisplay() {
        scheduledUpdate = true
    }

    private class ItemTrackingElement(val item: NEUInternalName, var current: Long, val target: Long?, val includeSack: Boolean) :
        TrackingElement() {

        override var line = generateLine()
        override fun similarElement(other: TrackingElement): Boolean {
            if (other !is ItemTrackingElement) return false
            return other.item == this.item
        }

        override fun atRemove() {
            itemTrackers[item]?.remove(this)
        }

        override fun atAdd() {
            itemTrackers.compute(item) { _, v ->
                v?.also { it.add(this) } ?: mutableListOf(this)
            }
        }

        override fun internalUpdate(amount: Number) {
            current += amount.toLong()
            if (target != null && current >= target) {
                if (shouldNotify) {
                    notify("${item.itemName} §adone")
                }
                if (shouldAutoDelete) {
                    delete()
                }
            }
            line = generateLine()
        }

        private fun generateLine() = listOf(
            Renderable.itemStack(item.getItemStack()),
            Renderable.string(item.itemName), Renderable.string((current.toString() + target?.let { " / $it" }) ?: ""),
        )
    }

    private abstract class TrackingElement {

        var shouldNotify = false
        var shouldAutoDelete = false

        fun update(amount: Number) {
            if (amount == 0) return
            internalUpdate(amount)
            updateDisplay()
        }

        fun notify(string: String) {
            shouldNotify = false
            SoundUtils.playPlingSound()
            LorenzUtils.sendTitle(string, 4.0.seconds)
        }

        fun delete() {
            tracker.remove(this)
        }

        abstract fun internalUpdate(amount: Number)

        abstract val line: List<Renderable>

        abstract fun similarElement(other: TrackingElement): Boolean

        abstract fun atRemove()

        abstract fun atAdd()
    }

}
