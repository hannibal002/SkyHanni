package at.hannibal2.skyhanni.test.command.track

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
import at.hannibal2.skyhanni.utils.renderables.StringRenderable
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.time.Duration.Companion.seconds

// todo currently this abstraction assumes that the tracked events have a location
//  if this is not in the case for an implementation in the future, this should be
//  abstracted further to `TrackCommand` and `TrackWorldCommand`
/**
 * Abstract class for commands that track specific events in the game.
 *
 * @param T The type of event to track, which must extend [CancellableWorldEvent].
 * @param K The type of identifier used to categorize the tracked events. Ideally, if possible, this should be an enum.
 * @param onlyOnSkyblock If true, the command will only work in SkyBlock.
 * @param commonName The singular name of the tracked event, used for command naming and display. (e.g., "sound", "particle").
 * @param commonNamePlural The plural name of the tracked event, used for command naming and display.
 *  Defaults to `commonName + "s"` (e.g., "sounds", "particles").
 */
abstract class TrackCommand<T : CancellableWorldEvent, K>(
    private val onlyOnSkyblock: Boolean = true,
    private val commonName: String,
    private val commonNamePlural: String = commonName + "s",
) {
    private data class Tracked<T>(val time: SimpleTimeMark, val event: T)

    protected abstract val config: TrackCommandConfig
    protected abstract val registerIgnoreBlock: LiteralCommandBuilder.() -> Unit

    // todo if there is ever a need for something besides a StringRenderable,
    //  this can and should be made to return a Renderable rather than a String
    abstract fun T.formatForDisplay(): String
    abstract fun T.formatForWorldRender(): String
    abstract fun T.shouldAcceptTrackableEvent(): Boolean
    abstract fun T.getTypeIdentifier(): K

    private var lastKeyToggle: SimpleTimeMark = SimpleTimeMark.farPast()
    private var isRecording = false
    private var display: List<Renderable> = emptyList()
    private var cutOffTime = SimpleTimeMark.farPast()
    private var startTime = SimpleTimeMark.farPast()
    private var worldTracked: Map<LorenzVec, List<T>> = emptyMap()

    private val ignoredTypes: MutableList<K> = mutableListOf()
    private val tracked = ConcurrentLinkedDeque<Tracked<T>>()
    private val commandName = "shtrack$commonNamePlural"

    private val recencyWindow get() = config.recencyWindow
    private val maxListLength get() = config.maxListLength

    protected fun handleIgnorable(ignorable: K) = if (ignorable in ignoredTypes) {
        ignoredTypes.remove(ignorable)
        ChatUtils.chat("§cRemoved $commonName '§e$ignorable§c' from the ignore list")
    } else {
        ignoredTypes.add(ignorable)
        ChatUtils.chat("§aAdded $commonName '§e$ignorable§c' to the ignore list")
    }

    private fun skyBlockCheck(): Boolean = if (onlyOnSkyblock && !SkyBlockUtils.inSkyBlock) {
        ChatUtils.userError("This command only works in SkyBlock!")
        false
    } else true

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

    private fun tryStartRecording(args: Array<String>) {
        if (!skyBlockCheck() || !alreadyRecordingCheck()) return

        val raw = args.firstOrNull()
        val durSec = raw?.toIntOrNull()
        if (raw != null && durSec == null) {
            ChatUtils.userError("Invalid duration: \"§e$raw§c\" isn’t a number")
            return
        }

        isRecording = true
        tracked.clear()
        startTime = SimpleTimeMark.now()
        cutOffTime = if (durSec != null) {
            ChatUtils.chat("Now started tracking $commonNamePlural for $durSec seconds")
            durSec.seconds.fromNow()
        } else {
            ChatUtils.chat("Now started tracking $commonNamePlural until manually ended")
            SimpleTimeMark.farFuture()
        }
    }

    private fun tryPutTrackedInClipboard() {
        // The function must run after cutOffTime has passed to ensure thread safety
        if (cutOffTime.passedSince() <= 0.1.seconds) return

        val string = tracked.reversed().joinToString("\n") {
            "Time: ${it.time.passedSince().inWholeMilliseconds}  ${it.event}"
        }
        OSUtils.copyToClipboard(string)
        ChatUtils.chat("${tracked.size} $commonNamePlural copied into the clipboard!")
        tracked.clear()
        isRecording = false
    }

    private fun SkyHanniRenderWorldEvent.drawSingleInWorld(vec: LorenzVec, event: T) {
        drawDynamicText(vec, "§7§l${event.getTypeIdentifier()}", 0.8)
        drawDynamicText(
            vec.down(0.2),
            event.formatForWorldRender(),
            scaleMultiplier = 0.8,
        )
    }

    private fun SkyHanniRenderWorldEvent.drawMultipleInWorld(vec: LorenzVec, events: List<T>) {
        drawDynamicText(vec, "§e${events.size} $commonNamePlural", 0.8)
        var offset = 0.2
        events.groupBy { it.getTypeIdentifier() }.forEach { (groupName, events) ->
            drawDynamicText(vec.down(offset), "§7§l$groupName §7(§e${events.size}§7)", 0.8)
            offset += 0.2
        }
    }

    // Functions below are event handlers that will be called by
    // extending objects that are SkyHanniModules
    // <editor-fold desc="Event Handlers">
    open fun onTrackableEvent(event: T) {
        if (cutOffTime.isInPast()) return
        if (event.getTypeIdentifier() in ignoredTypes) return
        if (event.shouldAcceptTrackableEvent()) {
            tracked.addFirst(Tracked(startTime, event))
        }
    }

    open fun onKeyPress(event: KeyPressEvent) {
        if (event.keyCode != config.toggleKeybind) return
        if (lastKeyToggle.passedSince() < 1.seconds) return

        if (isRecording) endRecording()
        else tryStartRecording(emptyArray())
        lastKeyToggle = SimpleTimeMark.now()
    }

    open fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (cutOffTime.isInPast()) return
        for ((vec, eventList) in worldTracked) {
            if (eventList.isEmpty()) continue
            else if (eventList.size != 1) event.drawMultipleInWorld(vec, eventList)
            else event.drawSingleInWorld(vec, eventList.first())
        }
    }

    open fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (cutOffTime.isInPast()) return
        config.position.renderRenderables(display, posLabel = "Track $commonName log")
    }

    open fun onTick() {
        if (!isRecording) return

        val cutoff = SimpleTimeMark.now() - recencyWindow.seconds
        val trackedToDisplay = tracked.takeWhile { it.time > cutoff }
        display = trackedToDisplay.take(maxListLength).reversed().map { (_, event) ->
            StringRenderable(event.formatForDisplay())
        }
        worldTracked = trackedToDisplay.map { it.event }.groupBy { it.location }

        tryPutTrackedInClipboard()
    }

    open fun onCommandRegistration(event: CommandRegistrationEvent) {
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
    // </editor-fold>
}
