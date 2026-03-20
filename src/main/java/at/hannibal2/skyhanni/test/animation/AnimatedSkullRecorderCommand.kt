package at.hannibal2.skyhanni.test.animation

import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.StringUtils.pluralize
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings

object AnimatedSkullRecorderCommand {

    private val stopRecordingCoroutine = CoroutineSettings("animated skull recorder stop recording")
    private val statusMessageId by lazy { ChatUtils.getUniqueMessageId() }

    @Suppress("HandleEventInspection")
    fun handleEvent(event: CommandRegistrationEvent) {
        event.registerBrigadier("shskull") {
            description = "Records animated skull texture frames for animatedskulls.json"
            category = CommandCategory.DEVELOPER_DEBUG
            aliases = listOf("shrecordanmiation", "shanimation", "shrecani")

            literal("start") {
                for (mode in listOf(AnimatedSkullRecorder.RecordingMode.HEAD, AnimatedSkullRecorder.RecordingMode.PET)) {
                    literal(mode.name.lowercase()) {
                        callback { AnimatedSkullRecorder.startRecording(mode) }
                    }
                }

                literal("player") {
                    arg("name", BrigadierArguments.string()) { nameArg ->
                        callback {
                            AnimatedSkullRecorder.startRecording(
                                AnimatedSkullRecorder.RecordingMode.PLAYER,
                                trackedPlayer = getArg(nameArg),
                            )
                        }
                    }

                    callback {
                        ChatUtils.userError("Please specify a player name.")
                        ChatUtils.chat("  §e/shskull start player <name> §7- Start recording a player's head frames.", prefix = false)
                    }
                }

                callback {
                    ChatUtils.userError("Please specify a recording mode: head, pet, or player <name>")
                    ChatUtils.chat("  §e/shskull start <head|pet|player <name>> §7- Start recording skull frames.", prefix = false)
                }
            }

            coroutineLiteralCallback("stop", config = stopRecordingCoroutine) { AnimatedSkullRecorder.stopRecording() }

            literalCallback("status") { ChatUtils.chat(buildStatusMessage(), messageId = statusMessageId) }

            callback {
                ChatUtils.userError("Please specify a subcommand: start, stop, or status.")
                ChatUtils.chat("  §e/shskull start <head|pet|player <name>> §7- Start recording skull frames.", prefix = false)
                ChatUtils.chat("  §e/shskull stop §7- Stop recording and copy frames to clipboard.", prefix = false)
                ChatUtils.chat("  §e/shskull status §7- Show current recording status.", prefix = false)
            }
        }
    }

    private fun buildStatusMessage(): String {
        if (!AnimatedSkullRecorder.isRecording) return "§cNot recording."
        val state = AnimatedSkullRecorder.state
        return if (state.mode == AnimatedSkullRecorder.RecordingMode.PET) buildPetStatus(state)
        else buildTrackerStatus(state.tracker)
    }

    private fun buildPetStatus(state: AnimatedSkullRecorder.RecordingState): String {
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
        return "§aCurrently recording pets - $entityFormat, $frameFormat captured, $loopFormat completed, §b${minSamples}§a sample(s)/frame min."
    }

    private fun buildTrackerStatus(tracker: SkullFrameTracker): String {
        val frameCount = tracker.frames.size
        if (frameCount == 0) return "§aCurrently recording - no frames captured yet."
        return buildString {
            append("§aCurrently recording - §b$frameCount§a ${"frame".pluralize(frameCount)} captured")
            if (tracker.loopCount > 0) {
                append(", §b${tracker.loopCount}§a ${"loop".pluralize(tracker.loopCount)} completed")
                append(", §b${tracker.minSampleCount}§a sample(s)/frame")
                val avgServer = tracker.frames.map { it.serverTicks }.average()
                val avgClient = tracker.frames.map { it.clientTicks }.average()
                append(
                    " §7(avg §bserver§7: §f${String.format("%.1f", avgServer)} §7ticks, §bclient§7: §f${
                        String.format(
                            "%.1f",
                            avgClient,
                        )
                    } §7ticks/frame)",
                )
            }
            if (tracker.verificationErrors > 0) append(", §c${tracker.verificationErrors}§a verification error(s)")
            append(".")
        }
    }
}
