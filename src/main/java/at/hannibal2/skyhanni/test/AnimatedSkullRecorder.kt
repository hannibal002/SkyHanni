package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.storage.Resettable
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ClipboardUtils
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.getPlayerEntities
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullOwner
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.StringUtils.pluralize
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.filterValuesNotNull
import at.hannibal2.skyhanni.utils.compat.getEquipmentSlots
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

@SkyHanniModule
object AnimatedSkullRecorder {

    enum class RecordingMode {
        NONE,
        HEAD,
        PET,
        PLAYER
    }

    private data class ArmorStandRecording(
        val entityId: Int,
        val displayName: String,
        val frames: LinkedHashSet<String> = linkedSetOf(),
    )

    private data class RecordingState(
        var mode: RecordingMode = RecordingMode.NONE,
        var trackedPlayer: String = "",
        val frames: LinkedHashSet<String> = linkedSetOf(),
        val petRecordings: LinkedHashMap<Int, ArmorStandRecording> = linkedMapOf(),
    ) : Resettable

    private var state: RecordingState = RecordingState()
    private val stopRecordingCoroutine = CoroutineSettings("animated skull recorder stop recording")

    val isRecording get() = state.mode != RecordingMode.NONE

    private fun ArmorStand.isPetTextureStand(): Boolean {
        // Pet stands are invisible
        if (!this.isInvisible) return false
        val equipment = getEquipmentSlots().filterValuesNotNull()
        // Pets should only have MAINHAND
        if (equipment.keys.size != 1) return false
        // Item should be a player head
        equipment[EquipmentSlot.MAINHAND]?.takeIf {
            it.item == Items.PLAYER_HEAD && it.getSkullTexture() != null
        } ?: return false
        return true
    }

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        val current = state.takeIf { it.mode != RecordingMode.NONE } ?: return
        if (Minecraft.getInstance().level == null) return state.reset()
        when (current.mode) {
            RecordingMode.NONE -> return
            RecordingMode.HEAD -> Minecraft.getInstance().player
                ?.getItemBySlot(EquipmentSlot.HEAD)
                ?.captureFrame()

            RecordingMode.PET -> EntityUtils.getEntitiesNearby<ArmorStand>(32.0) {
                it.isPetTextureStand()
            }.forEach { stand ->
                val recording = current.petRecordings.getOrPut(stand.id) {
                    ArmorStandRecording(stand.id, stand.name.string)
                }
                stand.getItemBySlot(EquipmentSlot.MAINHAND).captureFrameTo(recording.frames)
            }

            RecordingMode.PLAYER -> getPlayerEntities()
                .firstOrNull { it.name.string.equals(current.trackedPlayer, ignoreCase = true) }
                ?.getItemBySlot(EquipmentSlot.HEAD)
                ?.captureFrame()
        }
    }

    private fun ItemStack.captureFrame() = captureFrameTo(state.frames)

    private fun ItemStack.captureFrameTo(frames: LinkedHashSet<String>) {
        val uuid = getSkullOwner() ?: return
        val texture = getSkullTexture() ?: return
        frames.add("$uuid:$texture")
    }

    fun startRecording(mode: RecordingMode, trackedPlayer: String = "") {
        if (isRecording) {
            ChatUtils.chat("Restarting...")
            state.frames.clear()
        }
        state = RecordingState(mode, trackedPlayer)
        val label = if (mode == RecordingMode.PLAYER) "§e$trackedPlayer§a's head" else "§e${mode.name.lowercase()}§a"
        ChatUtils.chat("Started recording $label skull frames.")

        if (mode == RecordingMode.PET) ChatUtils.chat("§eMake sure no other armor stands are nearby.")
        ChatUtils.chat("Use §e/shskull stop §ato stop.")
    }

    suspend fun stopRecording() {
        val current = state.takeIf { it.mode != RecordingMode.NONE } ?: return ChatUtils.chat("§cNot currently recording.")

        val totalFrames = if (current.mode == RecordingMode.PET)
            current.petRecordings.values.sumOf { it.frames.size }
        else
            current.frames.size

        if (totalFrames == 0) {
            ChatUtils.chat("§cNo frames were captured.")
            return state.reset()
        }

        val formattedOutput = buildOutput(current)
        val copied = ClipboardUtils.copyToClipboardAsync(formattedOutput).await() ?: false
        if (!copied) return ChatUtils.chat("§cFailed to copy frames to clipboard.")
        else {
            if (current.mode == RecordingMode.PET) {
                val animated = current.petRecordings.values.count { it.frames.size > 1 }
                ChatUtils.chat("§a$animated animated entity/entities copied to clipboard.")
            } else {
                ChatUtils.chat("§a$totalFrames frame(s) copied to clipboard.")
            }
            state.reset()
        }
    }

    private fun buildOutput(state: RecordingState): String = when (state.mode) {
        RecordingMode.PET -> buildPetOutput(state)
        else -> state.frames.joinToString(",\n  ", "[\n  ", "\n]") { "\"$it\"" }
    }

    private fun buildPetOutput(state: RecordingState): String {
        val animated = state.petRecordings.values.filter { it.frames.size > 1 }
        return animated.joinToString(",\n  ", "{\n  ", "\n}") { recording ->
            val framesJson = recording.frames.joinToString(",\n      ", "[\n      ", "\n    ]") { "\"$it\"" }
            "\"${recording.entityId}\": {\n    \"displayName\": \"${recording.displayName}\",\n    \"frames\": $framesJson\n  }"
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shskull") {
            description = "Records animated skull texture frames for animatedskulls.json"
            category = CommandCategory.DEVELOPER_DEBUG
            aliases = listOf("shrecordanmiation", "shanimation", "shrecani")

            literal("start") {
                for (mode in listOf(RecordingMode.HEAD, RecordingMode.PET)) {
                    literal(mode.name.lowercase()) {
                        callback { startRecording(mode) }
                    }
                }

                literal("player") {
                    arg("name", BrigadierArguments.string()) { nameArg ->
                        callback { startRecording(RecordingMode.PLAYER, trackedPlayer = getArg(nameArg)) }
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

            coroutineLiteralCallback("stop", config = stopRecordingCoroutine) { stopRecording() }

            literalCallback("status") {
                val message = if (isRecording) {
                    if (state.mode == RecordingMode.PET) {
                        val entityCount = state.petRecordings.size
                        val frameCount = state.petRecordings.values.sumOf { it.frames.size }
                        if (entityCount == 0) "§aCurrently recording pets - no entities captured yet."
                        else {
                            val entityPluralized = "entity".pluralize(entityCount, "entities")
                            val framePluralized = "frame".pluralize(frameCount)

                            val entityFormat = "§b$entityCount§a $entityPluralized"
                            val frameFormat = "§b$frameCount§a total $framePluralized"
                            "§aCurrently recording pets - $entityFormat, $frameFormat captured so far."
                        }
                    } else {
                        val frameCount = state.frames.size
                        if (frameCount == 0) "§aCurrently recording - no frames captured yet."
                        else {
                            val framePluralized = "frame".pluralize(frameCount)
                            "§aCurrently recording - §b$frameCount§a $framePluralized captured so far."
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
