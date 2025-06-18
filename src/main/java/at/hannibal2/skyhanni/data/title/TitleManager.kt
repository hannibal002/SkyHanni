package at.hannibal2.skyhanni.data.title

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.title.CountdownTitleContext.Companion.fromTitleData
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.enumMapOf
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object TitleManager {

    private val titleLocationQueues: MutableMap<TitleLocation, CollectionUtils.OrderedQueue<TitleContext>> = enumMapOf()
    private val currentTitles: MutableMap<TitleLocation, TitleContext?> = enumMapOf()

    enum class CountdownTitleDisplayType(private val displayName: String) {
        WHOLE_SECONDS("Whole Seconds"),
        PARTIAL_SECONDS("Partial Seconds"),
        ;

        override fun toString() = displayName
    }

    enum class TitleLocation(
        private val displayName: String,
        val activationRequirement: () -> Boolean = { true },
    ) {
        GLOBAL("Global"),
        INVENTORY("Inventory", activationRequirement = { InventoryUtils.inInventory() }),
        ;

        override fun toString() = displayName
    }

    enum class TitleAddType(private val displayName: String) {
        FORCE_FIRST("Force First"),
        QUEUE("Queue"),
        ;

        override fun toString() = displayName
    }

    fun sendTitle(
        titleText: String,
        subtitleText: String? = null,
        duration: Duration = 5.seconds,
        location: TitleLocation = TitleLocation.GLOBAL,
        addType: TitleAddType = TitleAddType.QUEUE,
        weight: Double = 1.0,
        /**
         * Whether the title will be cleared from the queue when a WorldChangeEvent is triggered.
         */
        discardOnWorldChange: Boolean = true,
        /**
         * Prevent duplicate entries of the same title in the queue.
         */
        noDuplicates: Boolean = true,
        /**
         * Only provide these parameters if you want to use a countdown title.
         * countDownDisplayType being not null determines code path.
         */
        countDownDisplayType: CountdownTitleDisplayType? = null,
        countDownInterval: Duration = 1.seconds,
        onInterval: () -> Unit = {},
        onFinish: () -> Unit = {},
        /**
         * How long the title will 'stick around' for after the countdown is done.
         */
        loomDuration: Duration = 250.milliseconds,
    ): TitleContext? {
        val newTitle = TitleContext(titleText, subtitleText, duration, weight).let {
            when (countDownDisplayType) {
                null -> it
                else -> it.fromTitleData(
                    countDownDisplayType,
                    countDownInterval,
                    loomDuration,
                    discardOnWorldChange,
                    onInterval,
                    onFinish,
                )
            }
        }

        val targetQueue = titleLocationQueues.getOrPut(location) { CollectionUtils.OrderedQueue() }
        if (targetQueue.any { it.item == newTitle } && noDuplicates) return null

        val weightOverride = if (addType == TitleAddType.FORCE_FIRST) Double.MAX_VALUE else weight
        targetQueue.add(newTitle, weightOverride)

        return newTitle
    }

    fun conditionallyStopTitle(
        location: TitleLocation? = TitleLocation.GLOBAL,
        condition: (String) -> Boolean,
    ) = when (location) {
        null -> currentTitles.values.filterNotNull()
            .filter { condition(it.getTitleText()) }
            .forEach { it.stop() }

        else -> currentTitles[location]?.let { title ->
            if (condition(title.getTitleText())) title.stop()
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shsendtitle") {
            description = "Display a title on the screen with the specified settings."
            category = CommandCategory.DEVELOPER_TEST
            callback { command(it, this.name) }
        }
        event.register("shsendinventorytitle") {
            description = "Display a title on the inventory screen with the specified settings."
            category = CommandCategory.DEVELOPER_TEST
            callback { command(it, this.name, TitleLocation.INVENTORY) }
        }
        event.register("shsendcountdowntitle") {
            description = "Display a countdown title on the screen with the specified settings."
            category = CommandCategory.DEVELOPER_TEST
            callback { command(it, this.name, countdown = true) }
        }
    }

    private fun command(args: Array<String>, command: String, location: TitleLocation = TitleLocation.GLOBAL, countdown: Boolean = false) {
        if (args.getOrNull(0) == "reset") {
            titleLocationQueues.clear()
            for (context in currentTitles.values) {
                context?.stop()
            }
            ChatUtils.chat("Reset all active titles!")
            return
        }
        if (args.size < 2) {
            ChatUtils.userError("Usage: /$command <duration> <text ..>")
            return
        }

        val duration = TimeUtils.getDurationOrNull(args[0]) ?: run {
            ChatUtils.userError("Invalid duration format `${args[0]}`! Use e.g. 10s, or 20m or 30h")
            return
        }

        val raw = args.drop(1).joinToString(" ").replace("&", "§")
        val parts = raw.split("<br>")
        val titleText = "§6" + parts.first()
        val subTitle = parts.drop(1).takeIf { it.isNotEmpty() }?.joinToString(" ")

        sendTitle(
            titleText,
            subtitleText = subTitle,
            duration = duration,
            location,
            countDownDisplayType = if (countdown) CountdownTitleDisplayType.PARTIAL_SECONDS else null
        )
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("TitleManager")
        event.addIrrelevant {
            add(
                "Title Location Queues" + titleLocationQueues.let { queues ->
                    queues.entries.joinToString { queue ->
                        "${queue.key}:\n" + buildString {
                            appendLine("Title Queue: ${queue.value.size}")
                            queue.value.forEach { queuedTitle ->
                                appendLine(queuedTitle.item.toString())
                            }
                        }
                    }
                },
            )
            add(
                "Current titles" + currentTitles.let { titles ->
                    titles.entries.joinToString("\n\n") { title ->
                        "${title.key}:\n" + buildString {
                            val titleItem = title.value ?: return@buildString
                            appendLine(titleItem.toString())
                        }
                    }
                },
            )
        }
    }

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        titleLocationQueues.forEach { queue ->
            val (location, titleQueue) = queue
            titleLocationQueues[location] = titleQueue.copyWithFilter { !it.discardOnWorldChange }
        }
        currentTitles.forEach { (location, titleContext) ->
            if (titleContext == null || !titleContext.discardOnWorldChange) return@forEach
            titleContext.stop()
            currentTitles[location] = null
            dequeueNextTitle(location)
        }
    }

    @HandleEvent
    fun onProfileJoin() {
        stop()
    }

    @HandleEvent
    fun onInventoryClose() {
        stop(TitleLocation.INVENTORY)
    }

    private fun stop(location: TitleLocation? = null) {
        when (location) {
            null -> currentTitles.values.filterNotNull().forEach { it.stop() }
            else -> currentTitles[location]?.stop()
        }
    }

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        TitleLocation.entries.filter {
            it.activationRequirement.invoke()
        }.forEach { location ->
            when (val currentTitle = currentTitles[location]) {
                null -> dequeueNextTitle(location)
                else -> {
                    val titleLocationQueue = titleLocationQueues[location]
                    titleLocationQueue?.getWaitingWeightOrNull()?.let { waitingWeight ->
                        if (waitingWeight < currentTitle.weight) return@let
                        if (!currentTitle.alive) return@let
                        if (!currentTitle.processRequeue()) return@let
                        titleLocationQueue.add(currentTitle, currentTitle.weight)
                        dequeueNextTitle(location)
                        return@forEach
                    }
                    if (currentTitle.alive) return@forEach
                    currentTitle.stop()
                    dequeueNextTitle(location)
                }
            }
        }
        // Watchdog
        TitleLocation.entries.forEach {
            currentTitles[it]?.start()
            if (currentTitles[it]?.alive == false) {
                currentTitles[it]?.stop()
                currentTitles[it] = null
            }
        }
    }

    private fun dequeueNextTitle(location: TitleLocation) {
        val titleQueue = titleLocationQueues[location]
        val title = titleQueue?.pollOrNull()
        currentTitles[location] = title
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (InventoryUtils.inInventory()) return
        val globalTitle = currentTitles[TitleLocation.GLOBAL] ?: return
        globalTitle.tryRenderGlobalTitle()
    }

    @HandleEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!InventoryUtils.inInventory()) return
        val inventoryTitle = currentTitles[TitleLocation.INVENTORY] ?: return
        inventoryTitle.tryRenderInventoryTitle()
    }
}
