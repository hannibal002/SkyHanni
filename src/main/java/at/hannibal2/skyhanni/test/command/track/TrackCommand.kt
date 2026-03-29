package at.hannibal2.skyhanni.test.command.track

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.LiteralCommandBuilder
import at.hannibal2.skyhanni.config.features.dev.TrackCommandConfig
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.minecraft.ClientDisconnectEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.time.Duration.Companion.seconds

/**
 * Abstract class for commands that track specific events in the game.
 *
 * @param T The type of event to track, which must extend [CancellableSkyHanniEvent].
 * @param K The type of identifier used to categorize the tracked events. Ideally, if possible, this should be an enum.
 * @param onlyOnSkyblock If true, the command will only work in SkyBlock.
 * @param commonName The singular name of the tracked event, used for command naming and display. (e.g., "sound", "particle").
 * @param commonNamePlural The plural name of the tracked event, used for command naming and display.
 *  Defaults to `commonName + "s"` (e.g., "sounds", "particles").
 */
abstract class TrackCommand<T : CancellableSkyHanniEvent, K>(
    private val onlyOnSkyblock: Boolean = true,
    protected val commonName: String,
    protected val commonNamePlural: String = commonName + "s",
) {

    private data class Tracked<T>(
        val event: T,
        private val manualTime: SimpleTimeMark? = null,
    ) {
        val time: SimpleTimeMark = manualTime ?: SimpleTimeMark.now()
    }

    protected abstract val config: TrackCommandConfig
    protected abstract val registerIgnoreBlock: LiteralCommandBuilder.() -> Unit

    internal abstract fun T.formatForDisplay(): Renderable
    internal abstract fun T.shouldAcceptTrackableEvent(): Boolean
    internal abstract fun T.getTypeIdentifier(): K

    private var lastKeyToggle: SimpleTimeMark = SimpleTimeMark.farPast()
    private var isRecording = false
    private var display: List<Renderable> = emptyList()
    private var cutOffTime = SimpleTimeMark.farPast()
    private var startTime = SimpleTimeMark.farPast()

    private val ignoredTypes: MutableList<K> = mutableListOf()
    private val tracked = ConcurrentLinkedDeque<Tracked<T>>()
    private val commandName = "shtrack$commonNamePlural"

    private val recencyWindow get() = config.recencyWindow
    private val maxListLength get() = config.maxListLength

    protected val isActive get() = !cutOffTime.isInPast()

    protected fun handleIgnorable(ignorable: K) = if (ignorable in ignoredTypes) {
        ignoredTypes.remove(ignorable)
        ChatUtils.chat("§cRemoved $commonName §e$ignorable §cfrom the ignore list")
    } else {
        ignoredTypes.add(ignorable)
        ChatUtils.chat("§aAdded $commonName §e$ignorable §ato the ignore list")
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
                "§cStill tracking §e$commonNamePlural§c, wait for the other tracking to complete before starting a new one, " +
                    "or type §e/$commandName end §cto end it prematurely",
            )
            false
        } else true
    }

    private fun tryStartRecording(durSec: Int? = null) {
        if (!skyBlockCheck() || !alreadyRecordingCheck()) return

        isRecording = true
        tracked.clear()
        startTime = SimpleTimeMark.now()
        cutOffTime = if (durSec != null) {
            ChatUtils.chat("§eNow started tracking §a$commonNamePlural §efor §b${durSec}s")
            durSec.seconds.fromNow()
        } else {
            ChatUtils.chat("§eNow started tracking $commonNamePlural §euntil manually ended")
            SimpleTimeMark.farFuture()
        }
    }

    private fun tryPutTrackedInClipboard() {
        // The function must run after cutOffTime has passed to ensure thread safety
        if (cutOffTime.passedSince() <= 0.1.seconds) return

        val string = tracked.reversed().joinToString("\n") {
            val isCancelled = it.event.isCancelled
            val cancelFormat = if (isCancelled) "❌" else "✅"
            "Time: ${it.time.passedSince().inWholeMilliseconds} $cancelFormat ${it.event}"
        }
        OSUtils.copyToClipboard(string)
        ChatUtils.chat("§b${tracked.size} §a$commonNamePlural §ecopied into the clipboard!")
        tracked.clear()
        isRecording = false
    }

    // Functions below are event handlers that will be called by
    // extending objects that are SkyHanniModules
    // <editor-fold desc="Event Handlers">
    open fun onTrackableEvent(event: T) {
        if (cutOffTime.isInPast()) return
        if (event.getTypeIdentifier() in ignoredTypes) return
        if (event.shouldAcceptTrackableEvent()) {
            tracked.addFirst(Tracked(event))
        }
    }

    open fun onDisconnect(event: ClientDisconnectEvent) {
        isRecording = false
    }

    open fun onKeyPress(event: KeyPressEvent) {
        if (event.keyCode != config.toggleKeybind) return
        if (lastKeyToggle.passedSince() < 1.seconds) return

        if (isRecording) endRecording()
        else tryStartRecording()
        lastKeyToggle = SimpleTimeMark.now()
    }

    open fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (cutOffTime.isInPast()) return
        config.position.renderRenderables(display, posLabel = "Track $commonName log")
    }

    /**
     * Called during the tick update with events within the recency window.
     * Override to perform extension-specific tick processing.
     *
     * @param recent The list of recent events within the recency window.
     */
    protected open fun onTickExtensions(recent: List<T>) {}

    open fun onTick() {
        if (!isRecording) return

        val cutoff = SimpleTimeMark.now() - recencyWindow.seconds
        val trackedToDisplay = tracked.takeWhile { it.time > cutoff }
        display = trackedToDisplay.take(maxListLength).reversed().map { (event) ->
            event.formatForDisplay()
        }
        onTickExtensions(trackedToDisplay.map { it.event })

        tryPutTrackedInClipboard()
    }

    open fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier(commandName) {
            description = "§eTracks the §a$commonNamePlural §efor the specified duration (in seconds) and copies it to the clipboard"
            category = CommandCategory.DEVELOPER_TEST
            literalCallback("end") { endRecording() }
            literal("ignore") {
                registerIgnoreBlock()
                literalCallback("list") {
                    val message = if (ignoredTypes.isEmpty()) "§cNo §eignored $commonName types"
                    else "§aIgnored $commonName §etypes:\n§e${ignoredTypes.joinToString(", ")}"
                    ChatUtils.chat(message, replaceSameMessage = true)
                }
                literalCallback("clear") {
                    ignoredTypes.clear()
                    ChatUtils.chat("§aCleared all ignored $commonName types", replaceSameMessage = true)
                }
                callback {
                    ChatUtils.chat("§cSpecify a $commonName type to ignore", replaceSameMessage = true)
                }
            }
            argCallback("seconds", BrigadierArguments.integer()) { seconds ->
                tryStartRecording(seconds)
            }
            simpleCallback {
                tryStartRecording()
            }
        }
    }
    // </editor-fold>
}
