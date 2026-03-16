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
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import com.mojang.brigadier.arguments.IntegerArgumentType
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.ItemStack

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
        var keyName: String? = null,
        var ticks: Int = 0,
        var trackedPlayer: String = "",
        val frames: LinkedHashSet<String> = linkedSetOf(),
    ) : Resettable

    private var state: RecordingState = RecordingState()
    private val stopRecordingCoroutine = CoroutineSettings("animated skull recorder stop recording")

    val isRecording get() = state.mode != RecordingMode.NONE

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        val current = state.takeIf { it.mode != RecordingMode.NONE } ?: return
        if (Minecraft.getInstance().level == null) return state.reset()
        when (current.mode) {
            RecordingMode.NONE -> return
            RecordingMode.HEAD -> Minecraft.getInstance().player
                ?.getItemBySlot(EquipmentSlot.HEAD)
                ?.captureFrame()

            RecordingMode.PET -> EntityUtils.getEntitiesNearby<ArmorStand>(32.0)
                .forEach { it.getItemBySlot(EquipmentSlot.HEAD).captureFrame() }

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

    fun startRecording(mode: RecordingMode, keyName: String? = null, ticks: Int = 1, trackedPlayer: String = "") {
        if (isRecording) {
            ChatUtils.chat("Restarting...")
            state.frames.clear()
        }
        state = RecordingState(mode, keyName, ticks, trackedPlayer)
        val label = if (mode == RecordingMode.PLAYER) "§e$trackedPlayer§a's head" else "§e${mode.name.lowercase()}§a"
        ChatUtils.chat("Started recording $label skull frames.")

        if (mode == RecordingMode.PET) ChatUtils.chat("§eMake sure no other armor stands are nearby.")
        ChatUtils.chat("Use §e/shskull stop §ato stop.")
    }

    suspend fun stopRecording() {
        val current = state.takeIf { it.mode != RecordingMode.NONE } ?: return ChatUtils.chat("§cNot currently recording.")
        if (current.frames.isEmpty()) return ChatUtils.chat("§cNo frames were captured.")

        val copied = ClipboardUtils.copyToClipboardAsync(buildOutput(current)).await() ?: false
        if (!copied) return ChatUtils.chat("§cFailed to copy frames to clipboard.")
        else {
            ChatUtils.chat("§a${current.frames.size} frame(s) copied to clipboard.")
            state.reset()
        }
    }

    private fun buildOutput(state: RecordingState): String = if (state.keyName != null) {
        """
        |"${state.keyName}": {
        |    "ticks": ${state.ticks},
        |    "textures": [
        |        ${state.frames.joinToString(",\n        ") { "\"$it\"" }}
        |    ]
        |}
        """.trimMargin()
    } else state.frames.joinToString(",\n  ", "[\n  ", "\n]") { "\"$it\"" }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shskull") {
            description = "Records animated skull texture frames for animatedskulls.json"
            category = CommandCategory.DEVELOPER_DEBUG

            literal("start") {
                for (mode in listOf(RecordingMode.HEAD, RecordingMode.PET)) {
                    literal(mode.name.lowercase()) {
                        callback { startRecording(mode) }
                        arg("key", BrigadierArguments.string()) { keyArg ->
                            callback { startRecording(mode, keyName = getArg(keyArg)) }
                            argCallback("ticks", IntegerArgumentType.integer(1)) { ticks ->
                                startRecording(mode, keyName = getArg(keyArg), ticks = ticks)
                            }
                        }
                    }
                }

                literal("player") {
                    arg("name", BrigadierArguments.string()) { nameArg ->
                        callback { startRecording(RecordingMode.PLAYER, trackedPlayer = getArg(nameArg)) }
                        arg("key", BrigadierArguments.string()) { keyArg ->
                            callback { startRecording(RecordingMode.PLAYER, keyName = getArg(keyArg), trackedPlayer = getArg(nameArg)) }
                            argCallback("ticks", IntegerArgumentType.integer(1)) { ticks ->
                                startRecording(
                                    RecordingMode.PLAYER,
                                    keyName = getArg(keyArg),
                                    ticks = ticks,
                                    trackedPlayer = getArg(nameArg),
                                )
                            }
                        }
                    }
                }
            }

            coroutineLiteralCallback("stop", config = stopRecordingCoroutine) { stopRecording() }

            literalCallback("status") {
                if (isRecording) ChatUtils.chat("§aCurrently recording.") else ChatUtils.chat("§cNot recording.")
            }
        }
    }
}
