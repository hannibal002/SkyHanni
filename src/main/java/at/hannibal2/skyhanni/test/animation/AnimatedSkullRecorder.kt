package at.hannibal2.skyhanni.test.animation

import at.hannibal2.skyhanni.config.storage.Resettable
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ClipboardUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullOwner
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullSignature
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.StringUtils.pluralize
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.filterValuesNotNull
import at.hannibal2.skyhanni.utils.compat.getEquipmentSlots
import com.google.gson.GsonBuilder
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object AnimatedSkullRecorder {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    private data class TrackerOutput(val serverTicks: Int, val clientTicks: Int, val textures: List<String>)
    private data class PetEntryOutput(val displayName: String, val frames: List<SkullFrameTracker.FrameRecord>)

    enum class RecordingMode {
        NONE,
        HEAD,
        PET,
        PLAYER
    }

    class ArmorStandRecording(
        val entityId: Int,
        val displayName: String,
        val tracker: SkullFrameTracker = SkullFrameTracker(),
    )

    data class RecordingState(
        var mode: RecordingMode = RecordingMode.NONE,
        var trackedPlayer: String = "",
        val tracker: SkullFrameTracker = SkullFrameTracker(),
        val petRecordings: LinkedHashMap<Pair<Int, String>, ArmorStandRecording> = linkedMapOf(),
    ) : Resettable

    var state: RecordingState = RecordingState()
        private set

    val isRecording get() = state.mode != RecordingMode.NONE

    fun ArmorStand.isPetTextureStand(): Boolean {
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

    fun ItemStack.getFrameTexture(): SkullFrameTracker.FrameRecord? = getSkullTexture()?.let { texture ->
        SkullFrameTracker.FrameRecord(
            uuid = getSkullOwner(),
            texture = texture,
            signature = getSkullSignature(),
            clientTicks = 0,
            serverTicks = 0,
        )
    }

    fun startRecording(mode: RecordingMode, trackedPlayer: String = "") {
        if (isRecording) ChatUtils.chat("Restarting...")
        state = RecordingState(mode, trackedPlayer)
        val label = if (mode == RecordingMode.PLAYER) "§e$trackedPlayer§a's head" else "§e${mode.name.lowercase()}§a"
        ChatUtils.chat("Started recording $label skull frames.")

        if (mode == RecordingMode.PET) ChatUtils.chat("§eMake sure no other armor stands are nearby.")
        ChatUtils.chat("Use §e/shskull stop §ato stop.")
    }

    suspend fun stopRecording() {
        val current = state.takeIf { it.mode != RecordingMode.NONE } ?: return ChatUtils.chat("§cNot currently recording.")

        val totalFrames = if (current.mode == RecordingMode.PET) current.petRecordings.values.sumOf {
            it.tracker.frames.size
        } else current.tracker.frames.size

        if (totalFrames == 0) {
            ChatUtils.chat("§cNo frames were captured.")
            return state.reset()
        }

        val formattedOutput = buildOutput(current)
        val copied = ClipboardUtils.copyToClipboardAsync(formattedOutput).await() ?: false
        if (!copied) return ChatUtils.chat("§cFailed to copy frames to clipboard.")

        if (current.mode == RecordingMode.PET) {
            val animated = current.petRecordings.values.count { it.tracker.frames.size > 1 }
            ChatUtils.chat("§a$animated animated entity/entities copied to clipboard.")
        } else {
            val tracker = current.tracker
            val framePluralized = "frame".pluralize(totalFrames)
            val loopSuffix = if (tracker.loopCount > 0) {
                val loopPluralized = "loop".pluralize(tracker.loopCount)
                " §7(${tracker.loopCount} $loopPluralized verified)"
            } else ""
            val errorSuffix = if (tracker.verificationErrors > 0)
                " §c(${tracker.verificationErrors} verification error(s)!)"
            else ""
            ChatUtils.chat("§a$totalFrames $framePluralized copied to clipboard.$loopSuffix$errorSuffix")
        }
        state.reset()
    }

    private fun buildOutput(state: RecordingState): String = when (state.mode) {
        RecordingMode.PET -> buildPetOutput(state)
        else -> buildTrackerOutput(state.tracker)
    }

    private fun buildTrackerOutput(tracker: SkullFrameTracker): String = with(tracker) {
        val uniformServer = uniformServerTicks
        val uniformClient = uniformClientTicks
        if (uniformServer != null && uniformClient != null) {
            gson.toJson(TrackerOutput(uniformServer, uniformClient, frames.map { it.fullTexture }))
        } else gson.toJson(frames)
    }

    private fun buildPetOutput(state: RecordingState): String = state.petRecordings.values
        .filter { it.tracker.frames.size > 1 }
        .associate { it.entityId.toString() to PetEntryOutput(it.displayName, it.tracker.frames) }
        .let { gson.toJson(it) }
}
