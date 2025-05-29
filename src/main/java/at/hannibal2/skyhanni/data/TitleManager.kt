package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.TitleManager.CountdownTitleContext.Companion.fromTitleData
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.farPast
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.now
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.CollectionUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.enumMapOf
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object TitleManager {

    private val titleLocationQueues: MutableMap<TitleLocation, CollectionUtils.OrderedQueue<TitleContext>> = enumMapOf()
    private val currentTitles: MutableMap<TitleLocation, TitleContext?> = enumMapOf()

    open class TitleContext(
        private var titleText: String = "",
        private var subtitleText: String? = null,
        var duration: Duration = 1.seconds,
        val weight: Double = 1.0,
        var discardOnWorldChange: Boolean = true,
    ) {
        var endTime: SimpleTimeMark? = null
        var hasBeenRequeued: Boolean = false

        open val alive get() = endTime != null && (endTime?.isInPast() == false)

        open fun getTitleText(): String = titleText
        open fun getSubtitleText(): String? = subtitleText
        open fun start() {
            if (endTime == null || endTime?.isInPast() == true) {
                endTime = now() + duration
            }
        }

        open fun stop() {
            endTime = farPast()
        }

        override fun equals(other: Any?): Boolean = this === other || other is TitleContext && this.dataEquivalent(other)
        override fun hashCode(): Int =
            titleText.hashCode() * 31 + (subtitleText?.hashCode() ?: 0) * 31 +
                duration.hashCode() * 31 + weight.hashCode()

        protected fun dataEquivalent(other: TitleContext): Boolean = titleText == other.titleText &&
            subtitleText == other.subtitleText &&
            duration == other.duration &&
            weight == other.weight
    }

    enum class CountdownTitleDisplayType(private val displayName: String) {
        WHOLE_SECONDS("Whole Seconds"),
        PARTIAL_SECONDS("Partial Seconds"),
        ;

        override fun toString() = displayName
    }

    private data class CountdownTitleContext(
        var formattedTitleText: String = "",
        var formattedSubtitleText: String? = null,
        var countdownDuration: Duration = 5.seconds,
        var displayType: CountdownTitleDisplayType = CountdownTitleDisplayType.WHOLE_SECONDS,
        var updateInterval: Duration = 1.seconds,
        /**
         * How long the title will 'stick around' for after the countdown is done.
         */
        var loomDuration: Duration = 250.milliseconds,
        var onInterval: () -> Unit = {},
        var onFinish: () -> Unit = {},
    ) : TitleContext() {

        override val alive get() = super.alive && (virtualEndTime?.isInFuture() == true) && isActive

        private var virtualEndTime: SimpleTimeMark? = null
        private var virtualTimeLeft: Duration = getTimeLeft()
        private val internalUpdateInterval: Duration = 100.milliseconds.takeIf { it < updateInterval } ?: updateInterval
        private var isActive: Boolean = false

        private fun String.formatCountdownString() = this
            .replace("%t", virtualTimeLeft.toString())
            .replace("%f", virtualTimeLeft.format())

        override fun getTitleText(): String = formattedTitleText.formatCountdownString()
        override fun getSubtitleText(): String? = formattedSubtitleText?.formatCountdownString()

        override fun start() {
            if (isActive) return
            isActive = true
            virtualEndTime = if (virtualEndTime == null) (now() + countdownDuration) else {
                virtualEndTime?.also {
                    endTime = it + loomDuration
                }
            }
            onIntervalOutward()
            onIntervalInternal()
        }

        override fun stop() {
            isActive = false
            super.stop()
            onFinish()
        }

        override fun equals(other: Any?): Boolean = this === other || other is CountdownTitleContext && this.dataEquivalent(other)
        override fun hashCode(): Int = formattedTitleText.hashCode() * 31 + (formattedSubtitleText?.hashCode() ?: 0) * 31 +
            countdownDuration.hashCode() * 31 + displayType.hashCode() * 31 +
            updateInterval.hashCode() * 31 + loomDuration.hashCode() * 31 +
            onInterval.hashCode() * 31 + onFinish.hashCode()

        private fun dataEquivalent(other: CountdownTitleContext): Boolean = super.dataEquivalent(other) &&
            countdownDuration == other.countdownDuration &&
            displayType == other.displayType &&
            updateInterval == other.updateInterval &&
            loomDuration == other.loomDuration &&
            onInterval == other.onInterval &&
            onFinish == other.onFinish

        private fun getTimeLeft(): Duration = when (displayType) {
            CountdownTitleDisplayType.WHOLE_SECONDS -> (virtualEndTime?.timeUntil()?.inWholeSeconds ?: 0).seconds
            CountdownTitleDisplayType.PARTIAL_SECONDS -> (virtualEndTime?.timeUntil()?.inPartialSeconds ?: 0.0).seconds
        }

        // TODO instead of run delayed, use tick event or similar. inaccuracies below one tick (50 ms) are not relevant imo
        private fun onIntervalOutward() {
            if (!alive) return
            onInterval()
            DelayedRun.runDelayed(updateInterval) { onIntervalOutward() }
        }

        private fun onIntervalInternal() {
            if (!alive) return stop()
            virtualTimeLeft = if (virtualEndTime?.isInFuture() == true) getTimeLeft() else Duration.ZERO
            DelayedRun.runDelayed(internalUpdateInterval) { onIntervalInternal() }
        }

        companion object {
            fun TitleContext.fromTitleData(
                displayType: CountdownTitleDisplayType,
                updateInterval: Duration,
                loomDuration: Duration,
                discardOnWorldChange: Boolean = true,
                onInterval: () -> Unit = {},
                onFinish: () -> Unit = {},
            ) = CountdownTitleContext(
                formattedTitleText = getTitleText(),
                countdownDuration = duration,
                formattedSubtitleText = getSubtitleText(),
                displayType = displayType,
                updateInterval = updateInterval,
                loomDuration = loomDuration,
                onInterval = onInterval,
                onFinish = onFinish,
            ).apply {
                this.discardOnWorldChange = discardOnWorldChange
            }
        }
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
        val title = "§6" + args.drop(1).joinToString(" ").replace("&", "§")

        sendTitle(
            title,
            subtitleText = null,
            duration = duration,
            location,
            countDownDisplayType = if (countdown) CountdownTitleDisplayType.PARTIAL_SECONDS else null,
        )
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("TitleManager")
        event.addIrrelevant {
            add(
                "Title Location Queues" + titleLocationQueues.let { queues ->
                    queues.entries.joinToString("\n\n") { queue ->
                        "${queue.key}:\n" + buildString {
                            append("Title Queue: ${queue.value.size}\n")
                            queue.value.forEach { title ->
                                val titleItem = title.item
                                append("Title: ${titleItem.getTitleText()}\n")
                                append("Subtitle: ${titleItem.getSubtitleText()}\n")
                                append("Duration: ${titleItem.duration.inWholeSeconds}s\n")
                                append("Weight: ${titleItem.weight}\n")
                                append("End Time: ${titleItem.endTime?.timeUntil()?.inWholeSeconds ?: 0.0}s\n")
                            }
                        }
                    }
                },
            )
            add(
                "Current titles" + currentTitles.let { titles ->
                    titles.entries.joinToString("\n\n") { title ->
                        "${title.key}:\n" + buildString {
                            val titleItem = title.value
                            append("Title: ${titleItem?.getTitleText()}\n")
                            append("Subtitle: ${titleItem?.getSubtitleText()}\n")
                            append("Duration: ${titleItem?.duration?.inWholeSeconds}s\n")
                            append("Weight: ${titleItem?.weight}\n")
                            append("End Time: ${titleItem?.endTime?.timeUntil()?.inWholeSeconds}s\n")
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
    fun onProfileJoin(event: ProfileJoinEvent) {
        stop()
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
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
                        if (waitingWeight > currentTitle.weight) {
                            if (currentTitle.alive && currentTitle.endTime?.isInFuture() == true) {
                                currentTitle.duration = currentTitle.endTime?.timeUntil() ?: Duration.ZERO
                                if (currentTitle.duration > Duration.ZERO && !currentTitle.hasBeenRequeued) {
                                    currentTitle.hasBeenRequeued = true
                                    titleLocationQueue.add(currentTitle, currentTitle.weight)
                                }
                            }
                            dequeueNextTitle(location)
                            return@forEach
                        }
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

    // TODO move function inside title context class
    private fun TitleContext.tryRenderGlobalTitle() {
        val gui = SkyHanniMod.feature.gui
        val position = gui.titlePosition
        val guiWidth = GuiScreenUtils.scaledWindowWidth

        val mainScalar = position.scale * 3.0
        val subScalar = mainScalar * 0.75f

        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0)
        DrawContextUtils.pushMatrix()

        val mainTextRenderable = Renderable.string(
            getTitleText(),
            scale = mainScalar,
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
        )

        val subtitleRenderable: Renderable? = getSubtitleText()?.let {
            Renderable.string(
                it,
                scale = subScalar,
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
            )
        }

        val targetRenderable = if (subtitleRenderable == null) mainTextRenderable
        else Renderable.verticalContainer(
            listOf(mainTextRenderable, subtitleRenderable),
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
            verticalAlign = RenderUtils.VerticalAlignment.CENTER,
        )

        val renderableWidth = targetRenderable.width
        val renderableHeight = targetRenderable.height

        val posX = (guiWidth - renderableWidth) / 2
        var posY = position.y
        // moving the display to the bottom half of your screen is futile
        if (posY < 0) {
            posY = 100
        }
        if (posX != position.x || posY != position.y) {
            position.set(Position(posX, posY, scale = position.scale))
        }

        DrawContextUtils.translate(posX.toFloat(), posY.toFloat(), 0f)
        targetRenderable.renderXYAligned(0, 0, renderableWidth, renderableHeight)
        DrawContextUtils.popMatrix()

        GuiEditManager.add(position, "Title", targetRenderable.width, targetRenderable.height)
    }

    @HandleEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!InventoryUtils.inInventory()) return
        val inventoryTitle = currentTitles[TitleLocation.INVENTORY] ?: return
        inventoryTitle.tryRenderInventoryTitle()
    }

    private fun TitleContext.tryRenderInventoryTitle() {
        val gui = Minecraft.getMinecraft().currentScreen as? GuiContainer ?: return

        val baseStringRenderable = Renderable.string(getTitleText(), 1.5)
        val stringRenderable = when (getSubtitleText()) {
            null -> baseStringRenderable
            else -> {
                val displaySubText = getSubtitleText() ?: return
                Renderable.verticalContainer(
                    listOf(
                        baseStringRenderable,
                        Renderable.string(
                            displaySubText,
                            scale = 1.0,
                            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                        ),
                    ),
                    horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                )
            }
        }

        val heightTranslation = when (getSubtitleText()) {
            null -> 100f
            else -> 150f
        }

        DrawContextUtils.pushMatrix()
        DrawContextUtils.translate(0f, -(heightTranslation), 500f)
        Renderable.drawInsideRoundedRect(
            stringRenderable,
            ColorUtils.TRANSPARENT_COLOR,
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
            verticalAlign = RenderUtils.VerticalAlignment.CENTER,
        ).renderXYAligned(0, 0, gui.width, gui.height)

        DrawContextUtils.translate(0f, heightTranslation, -500f)
        DrawContextUtils.popMatrix()
    }
}
