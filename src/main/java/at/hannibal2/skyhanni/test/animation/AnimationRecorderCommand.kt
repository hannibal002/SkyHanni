package at.hannibal2.skyhanni.test.animation

import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.test.DevApi
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import at.hannibal2.skyhanni.utils.StringUtils.pluralize
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import java.util.Locale

object AnimationRecorderCommand {

    private val stopRecordingCoroutine = CoroutineSettings("animation recorder stop recording")
    private val statusMessageId by lazy { ChatUtils.getUniqueMessageId() }
    private val config get() = DevApi.config.devTool.recordAnimations

    @Suppress("HandleEventInspection")
    fun handleEvent(event: CommandRegistrationEvent) {
        event.registerBrigadier("shskull") {
            description = "Records animated texture frames for animations.json"
            category = CommandCategory.DEVELOPER_DEBUG
            aliases = listOf("shrecordanmiation", "shanimation", "shrecani")

            literal("start") {
                for (mode in listOf(AnimationState.RecordingMode.HEAD, AnimationState.RecordingMode.PET)) {
                    literal(mode.name.lowercase()) {
                        callback {
                            if (!checkEnabled()) return@callback
                            AnimationState.startRecording(mode)
                            AnimationRecorder.clearDebugRenderables()
                        }
                    }
                }

                literal("player") {
                    arg("name", BrigadierArguments.string()) { nameArg ->
                        callback {
                            if (!checkEnabled()) return@callback
                            AnimationState.startRecording(
                                AnimationState.RecordingMode.PLAYER,
                                trackedPlayer = getArg(nameArg),
                            )
                            AnimationRecorder.clearDebugRenderables()
                        }
                    }

                    callback {
                        if (!checkEnabled()) return@callback
                        ChatUtils.userError("Please specify a player name.")
                        ChatUtils.chat("  §e/shskull start player <name> §7- Start recording a player's animation frames.", prefix = false)
                    }
                }

                callback {
                    if (!checkEnabled()) return@callback
                    ChatUtils.userError("Please specify a recording mode: head, pet, or player <name>")
                    ChatUtils.chat("  §e/shskull start <head|pet|player <name>> §7- Start recording animation frames.", prefix = false)
                }
            }

            coroutineLiteralCallback("stop", config = stopRecordingCoroutine) {
                if (!checkEnabled()) return@coroutineLiteralCallback
                AnimationState.stopRecording()
            }

            literalCallback("status") {
                if (!checkEnabled()) return@literalCallback
                ChatUtils.chat(buildStatusMessage(), messageId = statusMessageId)
            }

            callback {
                if (!checkEnabled()) return@callback
                ChatUtils.userError("Please specify a subcommand: start, stop, or status.")
                ChatUtils.chat("  §e/shskull start <head|pet|player <name>> §7- Start recording animation frames.", prefix = false)
                ChatUtils.chat("  §e/shskull stop §7- Stop recording and copy frames to clipboard.", prefix = false)
                ChatUtils.chat("  §e/shskull status §7- Show current recording status.", prefix = false)
            }
        }
    }

    private fun checkEnabled(): Boolean {
        if (config.enabled.get()) return true
        ChatUtils.clickableChat(
            "The /shskull command is disabled. Click here to enable it in the dev tool config!",
            onClick = { config::enabled.jumpToEditor() },
            hover = "Click to open the dev tool config",
            replaceSameMessage = true,
        )
        return false
    }

    private fun buildStatusMessage(): String {
        if (!AnimationState.isRecording) return "§cNot recording."
        val state = AnimationState.state
        return if (state.mode == AnimationState.RecordingMode.PET) buildPetStatus(state)
        else buildTrackerStatus(state.tracker)
    }

    private fun buildPetStatus(state: AnimationState.RecordingState): String {
        val entityCount = state.petRecordings.size
        if (entityCount == 0) return "§aCurrently recording pets - no entities captured yet."

        val frameCount = state.petRecordings.values.sumOf { it.tracker.frames.size }
        val loopCount = state.petRecordings.values.sumOf { it.tracker.loopCount }
        val minSamples = state.petRecordings.values
            .filter { it.tracker.loopCount > 0 }
            .minOfOrNull { it.tracker.minSampleCount } ?: 0

        val entityFormat = "§b$entityCount§a ${"entity".pluralize(entityCount, "entities")}"
        val frameFormat = "§b$frameCount§a total ${"frame".pluralize(frameCount)}"
        val loopFormat = "§b$loopCount§a ${"loop".pluralize(loopCount)}"
        return "§aCurrently recording pets - $entityFormat, $frameFormat captured, " +
            "$loopFormat completed, §b$minSamples§a sample(s)/frame min."
    }

    private fun buildTrackerStatus(tracker: AnimationFrameTracker): String {
        val frameCount = tracker.frames.size
        if (frameCount == 0) return "§aCurrently recording - no frames captured yet."
        return buildString {
            append("§aCurrently recording - §b$frameCount§a ${"frame".pluralize(frameCount)} captured")
            if (tracker.loopCount > 0) {
                append(", §b${tracker.loopCount}§a ${"loop".pluralize(tracker.loopCount)} completed")
                append(", §b${tracker.minSampleCount}§a sample(s)/frame")
                val avgServer = tracker.frames.map { it.serverTicks }.average()
                val avgClient = tracker.frames.map { it.clientTicks }.average()
                val serverFormat = String.format(Locale.getDefault(), "%.1f", avgServer)
                val clientFormat = String.format(Locale.getDefault(), "%.1f", avgClient)
                append(" §7(avg §bserver§7: §f$serverFormat §7ticks, §bclient§7: §f$clientFormat §7ticks/frame)")
            }
            if (tracker.verificationErrors > 0) append(", §c${tracker.verificationErrors}§a verification error(s)")
            append(".")
        }
    }
}
