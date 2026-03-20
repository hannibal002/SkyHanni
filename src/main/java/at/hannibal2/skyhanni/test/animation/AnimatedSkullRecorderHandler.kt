package at.hannibal2.skyhanni.test.animation

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.minecraftevents.ClientEvents
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.events.minecraft.ServerTickEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.animation.AnimatedSkullRecorder.getFrameTexture
import at.hannibal2.skyhanni.test.animation.AnimatedSkullRecorder.isPetTextureStand
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.EntityUtils.getPlayerEntities
import at.hannibal2.skyhanni.utils.MobUtils
import at.hannibal2.skyhanni.utils.StringUtils.pluralize
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand

@SkyHanniModule
object AnimatedSkullRecorderHandler {

    private val stopRecordingCoroutine = CoroutineSettings("animated skull recorder stop recording")
    private var lastServerTick = 0L

    private fun recordFrame(serverTick: Long, clientTick: Int) {
        val current = AnimatedSkullRecorder.state.takeIf { it.mode != AnimatedSkullRecorder.RecordingMode.NONE } ?: return
        if (Minecraft.getInstance().level == null) return AnimatedSkullRecorder.state.reset()
        when (current.mode) {
            AnimatedSkullRecorder.RecordingMode.NONE -> return
            AnimatedSkullRecorder.RecordingMode.HEAD -> {
                val frame = Minecraft.getInstance().player?.getItemBySlot(EquipmentSlot.HEAD)?.getFrameTexture()
                current.tracker.record(serverTick, clientTick, frame)
            }

            AnimatedSkullRecorder.RecordingMode.PET -> EntityUtils.getEntitiesNearby<ArmorStand>(32.0) {
                it.isPetTextureStand()
            }.forEach { stand ->
                val displayName = MobUtils.getArmorStandByRangeAll(stand, 2.0).firstOrNull {
                    it.cleanName().startsWith("[Lv")
                }?.name?.string ?: stand.name.string
                val recording = current.petRecordings.getOrPut(stand.id to displayName) {
                    AnimatedSkullRecorder.ArmorStandRecording(stand.id, displayName)
                }
                stand.getItemBySlot(EquipmentSlot.MAINHAND).getFrameTexture()?.let {
                    recording.tracker.record(serverTick, clientTick, it)
                }
            }

            AnimatedSkullRecorder.RecordingMode.PLAYER -> {
                val frame = getPlayerEntities().firstOrNull {
                    it.name.string.equals(current.trackedPlayer, ignoreCase = true)
                }?.getItemBySlot(EquipmentSlot.HEAD)?.getFrameTexture()
                current.tracker.record(serverTick, clientTick, frame)
            }
        }
    }

    @HandleEvent
    fun onServerTick(event: ServerTickEvent) {
        lastServerTick = event.tick
        recordFrame(event.tick, ClientEvents.totalTicks)
    }

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        recordFrame(lastServerTick, ClientEvents.totalTicks)
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
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

            literalCallback("status") {
                val state = AnimatedSkullRecorder.state
                val message = if (AnimatedSkullRecorder.isRecording) {
                    if (state.mode == AnimatedSkullRecorder.RecordingMode.PET) {
                        val entityCount = state.petRecordings.size
                        val frameCount = state.petRecordings.values.sumOf { it.tracker.frames.size }
                        if (entityCount == 0) "§aCurrently recording pets - no entities captured yet."
                        else {
                            val entityPluralized = "entity".pluralize(entityCount, "entities")
                            val framePluralized = "frame".pluralize(frameCount)
                            val entityFormat = "§b$entityCount§a $entityPluralized"
                            val frameFormat = "§b$frameCount§a total $framePluralized"
                            "§aCurrently recording pets - $entityFormat, $frameFormat captured so far."
                        }
                    } else {
                        val tracker = state.tracker
                        val frameCount = tracker.frames.size
                        if (frameCount == 0) "§aCurrently recording - no frames captured yet."
                        else buildString {
                            val framePluralized = "frame".pluralize(frameCount)
                            append("§aCurrently recording - §b$frameCount§a $framePluralized captured")
                            if (tracker.loopCount > 0) {
                                val loopPluralized = "loop".pluralize(tracker.loopCount)
                                append(", §b${tracker.loopCount}§a $loopPluralized completed")
                            }
                            if (tracker.verificationErrors > 0) {
                                append(", §c${tracker.verificationErrors}§a verification error(s)")
                            }
                            append(".")
                        }
                    }
                } else "§cNot recording."
                ChatUtils.chat(message)
            }

            callback {
                ChatUtils.userError("Please specify a subcommand: start, stop, or status.")
                ChatUtils.chat("  §e/shskull start <head|pet|player <name>> §7- Start recording skull frames.", prefix = false)
                ChatUtils.chat("  §e/shskull stop §7- Stop recording and copy frames to clipboard.", prefix = false)
                ChatUtils.chat("  §e/shskull status §7- Show current recording status.", prefix = false)
            }
        }
    }
}
