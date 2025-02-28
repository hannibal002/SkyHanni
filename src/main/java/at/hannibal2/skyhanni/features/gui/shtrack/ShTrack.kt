package at.hannibal2.skyhanni.features.gui.shtrack

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.CollectionApi
import at.hannibal2.skyhanni.api.CollectionApi.getMultipleMap
import at.hannibal2.skyhanni.api.HotmApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.ComplexCommand
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.SackApi.getAmountInSacks
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.HypixelJoinEvent
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.ProfileLeaveEvent
import at.hannibal2.skyhanni.events.mining.PowderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.CollectionUtils.move
import at.hannibal2.skyhanni.utils.CommandArgument
import at.hannibal2.skyhanni.utils.CommandContextAwareObject
import at.hannibal2.skyhanni.utils.CommandUtils
import at.hannibal2.skyhanni.utils.CommandUtils.ItemGroup
import at.hannibal2.skyhanni.utils.CommandUtils.numberCalculate
import at.hannibal2.skyhanni.utils.InventoryUtils.getAmountInInventory
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.json.BaseGsonBuilder
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableTooltips
import com.google.gson.JsonElement
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

@SkyHanniModule
object ShTrack {

    private val config get() = SkyHanniMod.feature.gui.shTrackConfig

    @HandleEvent
    fun register(event: CommandRegistrationEvent) {
        event.registerComplex<ContextObject>("shtrack") {
            description = "Track any quantity"
            category = CommandCategory.USERS_ACTIVE
            specifiers = shTrackSpecifiers + shTrackItemsSpecifiers + globalSpecifiers
            context = { ContextObject(it) }
        }
        event.registerComplex<ContextObject>("shtrackitem") {
            description = "Track any item"
            category = CommandCategory.USERS_ACTIVE
            specifiers = shTrackItemsSpecifiers + globalSpecifiers
            aliases = listOf("shtrackitems")
            context = { ContextObject(it).apply { state = ContextObject.StateType.ITEM } }
        }
        event.registerComplex<ContextObject>("shtrackcollection") {
            description = "Tracks your collection gain over time"
            category = CommandCategory.MAIN
            specifiers = globalSpecifiers
            aliases = listOf("shtrackcollections")
            context = {
                ContextObject(it).apply {
                    state = ContextObject.StateType.ITEM
                    currentFetch = ContextObject.CurrentFetch.COLLECTION
                    multiItem = true
                }
            }
        }
    }

    val shTrackItemsSpecifiers = listOf<CommandArgument<ContextObject>>(
        CommandArgument("<number/calculation> - Sets the current amount", "-c", defaultPosition = -2) { a, c ->
            numberCalculate(a, c) { context, number ->
                context.currentAmount = number
            }
        },
        CommandArgument(
            "<> - Sets the current amount from sacks and inventory",
            "-s",
            validity = ShTrack::validIfItemState,
        ) { _, c ->
            c.currentFetch = ContextObject.CurrentFetch.SACKS
            0
        },
        CommandArgument(
            "<> - Sets the current amount from inventory", "-v",
            validity = ShTrack::validIfItemState,
        ) { _, c ->
            c.currentFetch = ContextObject.CurrentFetch.INVENTORY
            0
        },
        CommandArgument(
            "<> - Sets the current amount from collections (Does also do -m)", "-cc",
            validity = { validIfItemState(it) },
        ) { _, c ->
            c.currentFetch = ContextObject.CurrentFetch.COLLECTION
            c.multiItem = true
            0
        },
        CommandArgument(
            "<> - Uses all tiers of an item", "-m",
            validity = ShTrack::validIfItemState,
        ) { _, c ->
            c.multiItem = true
            0
        },
    )

    val shTrackSpecifiers = listOf<CommandArgument<ContextObject>>(
        CommandArgument(
            "<> - Sets the tracking type to items",
            "-i",
        ) { _, c ->
            c.state = ContextObject.StateType.ITEM
            0
        },
        CommandArgument(
            "<> - Sets the tracking type to powder",
            "-p",
        ) { _, c ->
            c.state = ContextObject.StateType.POWDER
            0
        },
        CommandArgument(
            "<powder> - Powder to be tracked.",
            defaultPosition = 0, validity = { it.state == ContextObject.StateType.POWDER },
            tabComplete = { s, _ -> HotmApi.PowderType.entries.filter { it.name.startsWith(s.uppercase()) }.map { it.name } },
        ) { a, c ->
            val entry = HotmApi.PowderType.getValue(a.first())
            c.item = entry
            1
        },
    )

    val globalSpecifiers = listOf<CommandArgument<ContextObject>>(
        CommandArgument("<> - Does save the tracker on game close", "-t") { _, c ->
            c.shouldSave = true
            0
        },
        CommandArgument("<number/calculation> - Sets the target amount", defaultPosition = 1) { a, c ->
            numberCalculate(
                a,
                c,
            ) { context, number -> context.targetAmount = number }
        },
        CommandArgument(
            "<item> - Item to be tracked",
            defaultPosition = 0,
            validity = ShTrack::validIfItemState,
            tabComplete = { s, c ->
                if (c.initializedBy.commandName != "shtrackcollection") CommandUtils.itemTabComplete(s)
                else CommandUtils.itemTabComplete(s, validItems = { CollectionApi.collectionValue?.get(it) != null }, suggestAtEmpty = true)
            },
        ) { a, c ->
            val r = if (c.initializedBy.commandName != "shtrackcollection") CommandUtils.itemCheck(a, c)
            else CommandUtils.itemCheck(
                a,
                c,
                validItems = { CollectionApi.collectionValue?.get(it) != null },
                notFoundResponse = { "$it collection not found. Try to open the collection inventory" },
            )
            r.second?.let { c.item = it }
            r.first
        },
        CommandArgument("<> - Does not replace the last equivalent tracking instance", "-d") { _, c ->
            c.allowDupe = true
            0
        },
        CommandArgument("<> - Does not delete the tracker on target completion", "-k") { _, c ->
            c.autoDelete = false
            0
        },
        CommandArgument("<> - Sends a notification on completion", "-n") { _, c ->
            c.notify = true
            0
        },
        CommandArgument("<> - Removes the percent value", "-np") { _, c ->
            c.showPercent = false
            0
        },
        CommandArgument("<> - Removes the gain value", "-ng") { _, c ->
            c.showGain = false
            0
        },
    )

    private fun validIfItemState(context: ContextObject) = context.state == ContextObject.StateType.ITEM

    class ContextObject(val initializedBy: ComplexCommand<ContextObject>) : CommandContextAwareObject {

        var allowDupe = false
        var autoDelete = true
        var notify = false
        var multiItem = false
        var shouldSave = false
        var showPercent = true
        var showGain = true

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

        private fun fetchCollection(it: NeuInternalName): Long = CollectionApi.getCollectionCounter(it) ?: run {
            errorMessage = "Collection amount is unknown. Open the specific collection and try again."
            0L
        }

        override fun post() {
            val result: TrackingElement<*> = compileState() ?: return
            result.shouldNotify = notify
            result.shouldAutoDelete = autoDelete
            result.shouldSave = shouldSave
            result.showPercent = showPercent
            result.showGain = showGain
            result.line = result.generateLine()
            val tracker = tracker ?: run {
                errorMessage = NullPointerException("tracker").message
                return
            }
            if (!allowDupe) {
                val index = tracker.indexOfFirst { result.similarElement(it) }
                if (index != -1) {
                    tracker[index] = result
                    return
                }
            }
            tracker.add(result)
        }

        private fun compileState(): TrackingElement<*>? = when (state) {
            StateType.ITEM -> {
                val current: Long
                val item = item
                val currentSelector: (NeuInternalName) -> Long = when (currentFetch) {
                    CurrentFetch.INVENTORY -> {
                        { it.getAmountInInventory().toLong() }
                    }

                    CurrentFetch.SACKS -> {
                        { it.getAmountInInventory().toLong() + it.getAmountInSacks().toLong() }
                    }

                    CurrentFetch.COLLECTION -> {
                        {
                            fetchCollection(it)
                        }
                    }

                    else -> {
                        { 0L }
                    }
                }
                val result: TrackingElement<*>?
                when (item) {
                    is ItemGroup -> {
                        current = currentAmount
                            ?: if (currentFetch == CurrentFetch.COLLECTION) fetchCollection(item.collection.toInternalName())
                            else item.items.keys.sumOf(currentSelector)
                        result = ItemGroupElement(item, current, targetAmount, currentFetch != CurrentFetch.INVENTORY)
                    }

                    is NeuInternalName -> {
                        if (multiItem) {
                            val base = NeuItems.getPrimitiveMultiplier(item)
                            current =
                                currentAmount?.let { it * base.amount } ?: if (currentFetch == CurrentFetch.COLLECTION) fetchCollection(
                                    base.internalName,
                                )
                                else base.internalName.getMultipleMap().entries.sumOf { currentSelector(it.key) * it.value }
                            result = ItemsStackElement(item, current, targetAmount, currentFetch != CurrentFetch.INVENTORY)
                        } else {
                            current = currentAmount ?: currentSelector(item)
                            result = ItemTrackingElement(item, current, targetAmount, currentFetch != CurrentFetch.INVENTORY)
                        }
                    }

                    else -> {
                        errorMessage = "No item specified"
                        result = null
                    }
                }
                result
            }

            StateType.POWDER -> {
                val type = item as? HotmApi.PowderType ?: run {
                    errorMessage = "No powder specified"
                    return null
                }
                val current = currentAmount ?: type.current
                PowderTrackingElement(type, current, targetAmount)
            }

            else -> {
                errorMessage = "No tracking type specified"
                null
            }
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

    val typeAdapter = object : TypeAdapter<TrackingElement<*>>() {
        override fun write(out: JsonWriter, value: TrackingElement<*>) {
            if (!value.shouldSave) {
                return
            }
            out.beginObject()
            value.toJson(out)
            out.endObject()
        }

        override fun read(reader: JsonReader): TrackingElement<*>? {
            reader.beginObject()

            val map = mutableMapOf<String, JsonElement>()

            while (reader.hasNext()) {
                println(reader.peek())
                val name = reader.nextName()
                val value = BaseGsonBuilder.finishedBase.fromJson<JsonElement>(reader, JsonElement::class.java)
                println(reader.peek())
                map[name] = value
            }

            reader.endObject()

            try {
                // New Tracking Elements need to be added to when below
                val tracker: TrackingElement<*> = when (map["type"]?.asString) {
                    PowderTrackingElement::class.simpleName -> PowderTrackingElement.fromJson(map)
                    ItemsStackElement::class.simpleName -> ItemsStackElement.fromJson(map)
                    ItemTrackingElement::class.simpleName -> ItemTrackingElement.fromJson(map)
                    else -> return null
                }
                tracker.applyMetaOptions(map)
                return tracker
            } catch (e: Throwable) {
                ErrorManager.logErrorWithData(
                    e, "Malformed Json",
                    "data" to map,
                )
                return null
            }
        }
    }

    val itemTrackers: MutableMap<NeuInternalName, MutableList<ItemTrackingInterface>> = mutableMapOf()
    val powderTracker = mutableListOf<PowderTrackingElement>()

    @HandleEvent
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
    fun onPowderGain(event: PowderEvent) {
        powderTracker.forEach {
            if (it.type == event.powder) {
                it.update(event.amount)
            }
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enable

    val tracker get() = ProfileStorageData.profileSpecific?.tracking

    private var display: Renderable = Renderable.placeholder(0, 0)
    private var hasGrab = false
    private var scheduledUpdate = false

    fun updateDisplay() {
        scheduledUpdate = true
    }

    @HandleEvent
    fun onProfileLeave(event: ProfileLeaveEvent) {
        tracker?.deactivate()
    }

    @HandleEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        tracker?.activate()
    }

    @HandleEvent
    fun onHypixelJoin(event: HypixelJoinEvent) {
        // Clear out values that where loaded via gson
        ProfileStorageData.playerSpecific?.profiles?.forEach { (_, profile) ->
            profile.tracking.deactivate()
        }
    }

    @HandleEvent
    fun onGuiRenderGuiOverlayRender(event: GuiRenderEvent) {
        if (!isEnabled()) return
        val tracker = tracker
        if (scheduledUpdate && tracker != null) {
            display = Renderable.verticalEditTable(
                tracker.map { it.line },
                onHover = {
                    if (!hasGrab) {
                        val track = tracker[it]
                        RenderableTooltips.setTooltipForRender(
                            track.generateHover().map { i -> Renderable.string(i) },
                            spacedTitle = true,
                        )
                        track.handleUserInput()
                    }
                },
                onStartGrab = { hasGrab = true },
                onEndGrab = { hasGrab = false },
                onDrop = { a, b ->
                    tracker.move(a, b)
                    updateDisplay()
                },
            )
            scheduledUpdate = false
        }
        config.position.renderRenderable(display, posLabel = "Tracker")
    }
}
