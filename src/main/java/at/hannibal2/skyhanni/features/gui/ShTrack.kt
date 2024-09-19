package at.hannibal2.skyhanni.features.gui

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.CollectionAPI
import at.hannibal2.skyhanni.api.HotmAPI
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandArgument
import at.hannibal2.skyhanni.config.commands.CommandContextAwareObject
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.data.SackAPI.getAmountInSacks
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.mining.PowderGainEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.CommandUtils.ItemGroup
import at.hannibal2.skyhanni.utils.CommandUtils.itemCheck
import at.hannibal2.skyhanni.utils.CommandUtils.numberCalculate
import at.hannibal2.skyhanni.utils.InventoryUtils.getAmountInInventory
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.PrimitiveItemStack
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ShTrack {

    private val config get() = SkyHanniMod.feature.gui.shTrackConfig

    val arguments = listOf<CommandArgument<ContextObject>>(
        CommandArgument("-i") { _, c -> c.state = ContextObject.StateType.ITEM; 0 },
        CommandArgument("-p") { _, c -> c.state = ContextObject.StateType.POWDER; 0 },
        CommandArgument(defaultPosition = 1) { a, c -> numberCalculate(a, c) { context, number -> context.targetAmount = number } },
        CommandArgument(defaultPosition = 0, validity = ::validIfItemState) { a, c ->
            val r = itemCheck(a, c)
            r.second?.let { c.item = it }
            r.first
        },
        CommandArgument(defaultPosition = 0, validity = { it.state == ContextObject.StateType.POWDER }) { a, c ->
            val entry = HotmAPI.PowderType.getValue(a.first())
            c.item = entry
            1
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

    init {
        ItemGroup("GOLD", "GOLD_INGOT" to 1, "GOLD_BLOCK" to 9, "ENCHANTED_GOLD" to 160, "ENCHANTED_GOLD_BLOCK" to 160 * 160)
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

        var item: Any? = null
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
                    val current: Long
                    val item = item
                    val currentSelector: (NEUInternalName) -> Long = when (currentFetch) {
                        CurrentFetch.INVENTORY -> {
                            { it.getAmountInInventory().toLong() }
                        }

                        CurrentFetch.SACKS -> {
                            { it.getAmountInInventory().toLong() + it.getAmountInSacks().toLong() }
                        }

                        CurrentFetch.COLLECTION -> {
                            {
                                CollectionAPI.getCollectionCounter(it) ?: run {
                                    errorMessage = "Collection amount is unknown"
                                    0L
                                }
                            }
                        }

                        else -> {
                            { 0L }
                        }
                    }
                    when (item) {
                        is ItemGroup -> {
                            current = currentAmount ?: item.items.keys.sumOf(currentSelector)
                            result = ItemGroupElement(item, current, targetAmount, currentFetch != CurrentFetch.INVENTORY)
                        }

                        is NEUInternalName -> {
                            current = currentAmount ?: currentSelector(item)
                            result = ItemTrackingElement(item, current, targetAmount, currentFetch != CurrentFetch.INVENTORY)
                        }

                        else -> {
                            errorMessage = "No item specified"
                            return
                        }
                    }
                }

                StateType.POWDER -> {
                    val type = item as? HotmAPI.PowderType ?: run {
                        errorMessage = "No powder specified"
                        return
                    }
                    val current = currentAmount ?: type.getCurrent()
                    result = PowderTrackingElement(type, current, targetAmount)
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
                    return
                }
            }
            tracker.add(result)
        }

        enum class StateType {
            ITEM,
            POWDER
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

    private val itemTrackers: MutableMap<NEUInternalName, MutableList<ItemTrackingInterface>> = mutableMapOf()
    private val powderTracker = mutableListOf<PowderTrackingElement>()

    private var display: Renderable = Renderable.placeholder(0, 0)

    @SubscribeEvent
    fun onGuiRenderGuiOverlayRender(event: GuiRenderEvent) {
        if (scheduledUpdate) {
            display = Renderable.verticalEditTable(
                tracker.map { it.line },
                onHover = {},
                onClick = {},
                onDrop = { a, b ->
                    tracker.move(a, b)
                    updateDisplay()
                },
            )
            scheduledUpdate = false
        }
        config.position.renderRenderable(display, posLabel = "Tracker")
    }

    @SubscribeEvent
    fun onItemAdd(event: ItemAddEvent) {
        val trackers = itemTrackers[event.internalName] ?: return
        if (event.source == ItemAddManager.Source.SACKS) {
            for (tracker in trackers) {
                if (!tracker.includeSack) continue
                tracker.itemChange(event.pStack)
            }
        } else {
            for (tracker in trackers) {
                tracker.itemChange(event.pStack)
            }
        }

    }

    @HandleEvent
    fun onPowderGain(event: PowderGainEvent) {
        powderTracker.forEach {
            if (it.type == event.powder) {
                it.update(event.amount)
            }
        }
    }

    private var scheduledUpdate = false

    fun updateDisplay() {
        scheduledUpdate = true
    }

    private interface ItemTrackingInterface {

        fun itemChange(item: PrimitiveItemStack)

        val includeSack: Boolean
    }

    private class ItemTrackingElement(val item: NEUInternalName, var current: Long, val target: Long?, override val includeSack: Boolean) :
        TrackingElement(), ItemTrackingInterface {

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
            Renderable.string(item.itemName),
            Renderable.string(current.toString() + ((target?.let { " / $it" }) ?: "")),
        )

        override fun itemChange(item: PrimitiveItemStack) {
            update(item.amount)
        }
    }

    private class ItemGroupElement(val group: ItemGroup, var current: Long, val target: Long?, override val includeSack: Boolean) :
        TrackingElement(), ItemTrackingInterface {

        override var line = generateLine()
        override fun similarElement(other: TrackingElement): Boolean {
            if (other !is ItemGroupElement) return false
            return other.group == this.group
        }

        override fun atRemove() {
            for (item in group.items.keys) {
                itemTrackers[item]?.remove(this)
            }
        }

        override fun atAdd() {
            for (item in group.items.keys) {
                itemTrackers.compute(item) { _, v ->
                    v?.also { it.add(this) } ?: mutableListOf(this)
                }
            }
        }

        override fun internalUpdate(amount: Number) {
            current += amount.toLong()
            if (target != null && current >= target) {
                if (shouldNotify) {
                    notify("${group.name} §adone")
                }
                if (shouldAutoDelete) {
                    delete()
                }
            }
            line = generateLine()
        }

        private fun generateLine() = listOf(
            Renderable.itemStack(group.icon.getItemStack()),
            Renderable.string(group.name),
            Renderable.string(current.toString() + ((target?.let { " / $it" }) ?: "")),
        )

        override fun itemChange(item: PrimitiveItemStack) {
            val multiple = group.items[item.internalName] ?: throw IllegalStateException("You should not be here!")
            update(item.amount * multiple)
        }
    }

    private class PowderTrackingElement(val type: HotmAPI.PowderType, var current: Long, val target: Long?) : TrackingElement() {

        override var line = generateLine()

        override fun internalUpdate(amount: Number) {
            current += amount.toLong()
            if (target != null && current >= target) {
                if (shouldNotify) {
                    notify("${type.displayName} §adone")
                }
                if (shouldAutoDelete) {
                    delete()
                }
            }
            line = generateLine()
        }

        private fun generateLine() = listOf(
            Renderable.itemStack(type.icon),
            Renderable.string(type.displayName),
            Renderable.string(current.toString() + ((target?.let { " / $it" }) ?: "")),
        )

        override fun similarElement(other: TrackingElement): Boolean {
            if (other !is PowderTrackingElement) return false
            return other.type == this.type
        }

        override fun atRemove() {
            powderTracker.remove(this)
        }

        override fun atAdd() {
            powderTracker.add(this)
        }

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

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enable

}

private fun <E> MutableList<E>.move(from: Int, to: Int) {
    val element = this[from]
    this.removeAt(from)
    add(to, element)
}
