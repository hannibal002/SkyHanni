package at.hannibal2.skyhanni.test.command.track

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.LiteralCommandBuilder
import at.hannibal2.skyhanni.config.features.dev.TrackCommandConfig
import at.hannibal2.skyhanni.events.CancellableWorldEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.renderables.Renderable
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

abstract class TrackCommand<T : CancellableWorldEvent, K>(
    private val onlyOnSkyblock: Boolean = true,
    private val commonName: String,
    private val commonNamePlural: String = commonName + "s",
) {

    abstract val config: TrackCommandConfig
    abstract val registerIgnoreBlock: LiteralCommandBuilder.() -> Unit

    abstract fun drawDisplay(tracked: List<Pair<Duration, T>>): List<Renderable>
    abstract fun onTrackable(event: T)
    abstract fun T.getTypeIdentifier(): K

    private var isRecording = false
    private var display: List<Renderable> = emptyList()
    var cutOffTime = SimpleTimeMark.farPast()
        private set
    var startTime = SimpleTimeMark.farPast()
        private set
    var worldTracked: Map<LorenzVec, List<T>> = emptyMap()
        private set

    private val ignoredTypes: MutableList<K> = mutableListOf()
    private val tracked = ConcurrentLinkedDeque<Pair<Duration, T>>()
    private val commandName = "shtrack$commonNamePlural"

    protected fun addTrackable(event: T) {
        if (cutOffTime.isInPast()) return
        tracked.addFirst(startTime.passedSince() to event)
    }

    protected fun handleIgnorable(ignorable: K) {
        if (ignorable in ignoredTypes) {
            ignoredTypes.remove(ignorable)
            ChatUtils.chat("§cRemoved $commonName '§e$ignorable§c' from the ignore list")
        } else {
            ignoredTypes.add(ignorable)
            ChatUtils.chat("§aAdded $commonName '§e$ignorable§c' to the ignore list")
        }
    }

    private fun skyBlockCheck(): Boolean {
        return if (onlyOnSkyblock && !SkyBlockUtils.inSkyBlock) {
            ChatUtils.userError("This command only works in SkyBlock!")
            false
        } else true
    }

    private fun endRecording() {
        if (!isRecording) ChatUtils.userError("Nothing to end")
        else cutOffTime = SimpleTimeMark.now()
    }

    private fun alreadyRecordingCheck(): Boolean {
        return if (isRecording) {
            ChatUtils.userError(
                "Still tracking $commonNamePlural, wait for the other tracking to complete before starting a new one, " +
                    "or type §e/$commandName end §cto end it prematurely",
            )
            false
        } else true
    }

    open fun earlyArgHandler(args: Array<String>, isRecording: Boolean): Boolean = false

    private fun tryStartRecording(args: Array<String>) {
        if (!skyBlockCheck()) return
        if (earlyArgHandler(args, isRecording)) return
        if (!alreadyRecordingCheck()) return

        isRecording = true
        tracked.clear()
        startTime = SimpleTimeMark.now()
        cutOffTime = args.firstOrNull()?.toInt()?.seconds?.let {
            ChatUtils.chat("Now started tracking $commonNamePlural for ${it.inWholeSeconds} Seconds")
            it.fromNow()
        } ?: run {
            ChatUtils.chat("Now started tracking $commonNamePlural until manually ended")
            SimpleTimeMark.farFuture()
        }
    }

    private fun tryPutTrackedInClipboard() {
        // The function must run after cutOffTime has passed to ensure thread safety
        if (cutOffTime.passedSince() <= 0.1.seconds) return

        val string = tracked.reversed().joinToString("\n") { "Time: ${it.first.inWholeMilliseconds}  ${it.second}" }
        val counter = tracked.size
        OSUtils.copyToClipboard(string)
        ChatUtils.chat("$counter $commonNamePlural copied into the clipboard!")
        tracked.clear()
        isRecording = false
    }

    private fun SkyHanniRenderWorldEvent.drawMultiple(
        vec: LorenzVec,
        events: List<T>,
    ) {
        drawDynamicText(vec, "§e${events.size} sounds", 0.8)
        var offset = 0.2
        events.groupBy { it.getTypeIdentifier() }.forEach { (groupName, events) ->
            drawDynamicText(vec.down(offset), "§7§l$groupName §7(§e${events.size}§7)", 0.8)
            offset += 0.2
        }
    }
    abstract fun SkyHanniRenderWorldEvent.drawSingle(vec: LorenzVec, event: T)

    @HandleEvent
    fun onTrackableEvent(event: T) {
        if (cutOffTime.isInPast()) return
        if (event.getTypeIdentifier() in ignoredTypes) return
        onTrackable(event)
    }

    @HandleEvent
    fun onKeyPress(event: KeyPressEvent) {
        if (event.keyCode != config.toggleKeybind) return
        else if (isRecording) endRecording()
        else tryStartRecording(emptyArray())
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (cutOffTime.isInPast()) return
        for ((vec, eventList) in worldTracked) {
            if (eventList.isEmpty()) continue
            else if (eventList.size != 1) event.drawMultiple(vec, eventList)
            else event.drawSingle(vec, eventList.first())
        }
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (cutOffTime.isInPast()) return
        config.position.renderRenderables(display, posLabel = "Track $commonName log")
    }

    @HandleEvent
    fun onTick() {
        if (!isRecording) return

        val trackedToDisplay = tracked.takeWhile { startTime.passedSince() - it.first < 3.seconds }
        display = drawDisplay(trackedToDisplay)
        worldTracked = trackedToDisplay.map { it.second }.groupBy { it.location }

        tryPutTrackedInClipboard()
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier(commandName) {
            description = "Tracks the $commonNamePlural for the specified duration (in seconds) and copies it to the clipboard"
            category = CommandCategory.DEVELOPER_TEST
            literal("end") {
                endRecording()
            }
            literal("ignore") {
                registerIgnoreBlock()
            }
            legacyCallbackArgs(::tryStartRecording)
        }
    }
}
