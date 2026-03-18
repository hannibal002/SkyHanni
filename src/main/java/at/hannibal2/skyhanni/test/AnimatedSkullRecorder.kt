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

    private data class RecordingState(
        var mode: RecordingMode = RecordingMode.NONE,
        var trackedPlayer: String = "",
        val frames: LinkedHashSet<String> = linkedSetOf(),
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
            }.forEach { it.getItemBySlot(EquipmentSlot.MAINHAND).captureFrame() }

            RecordingMode.PLAYER -> getPlayerEntities()
                .firstOrNull { it.name.string.equals(current.trackedPlayer, ignoreCase = true) }
                ?.getItemBySlot(EquipmentSlot.HEAD)
                ?.captureFrame()
        }
    }

    private fun ItemStack.captureFrame() {
        val uuid = getSkullOwner() ?: return
        val texture = getSkullTexture() ?: return
        state.frames.add("$uuid:$texture")
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
        if (current.frames.isEmpty()) {
            ChatUtils.chat("§cNo frames were captured.")
            return state.reset()
        }

        val formattedOutput = buildOutput(current)
        val copied = ClipboardUtils.copyToClipboardAsync(formattedOutput).await() ?: false
        if (!copied) return ChatUtils.chat("§cFailed to copy frames to clipboard.")
        else {
            ChatUtils.chat("§a${current.frames.size} frame(s) copied to clipboard.")
            state.reset()
        }
    }

    private fun buildOutput(state: RecordingState): String = state.frames.joinToString(",\n  ", "[\n  ", "\n]") { "\"$it\"" }

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
                    val frameCount = state.frames.size
                    if (frameCount == 0) "§aCurrently recording - no frames captured yet."
                    else "§aCurrently recording - §b$frameCount§a frame(s) captured so far."
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
