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

object AnimationState {

    private val gson = GsonBuilder().setPrettyPrinting().create()

    private data class AnimationOutput(
        val textures: List<String>,
        val ticksPerTexture: List<Int>,
        private val ticks: Int = 0,
    )

    enum class RecordingMode {
        NONE,
        HEAD,
        PET,
        PLAYER,
    }

    class ArmorStandRecording(
        val displayName: String,
        val tracker: AnimationFrameTracker = AnimationFrameTracker(),
    )

    data class RecordingState(
        var mode: RecordingMode = RecordingMode.NONE,
        var trackedPlayer: String = "",
        val tracker: AnimationFrameTracker = AnimationFrameTracker(),
        val petRecordings: LinkedHashMap<String, ArmorStandRecording> = linkedMapOf(),
        var skinId: String? = null,
        var skinColor: String? = null,
    ) : Resettable {
        val skinName: String? get() = skinId?.let { id ->
            if (skinColor != null) "${id}_${skinColor!!.replace(" ", "_").uppercase()}" else id
        }
    }

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

    fun ItemStack.getFrameTexture(): AnimationFrameTracker.FrameRecord? = getSkullTexture()?.let { texture ->
        AnimationFrameTracker.FrameRecord(
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
        val label = if (mode == RecordingMode.PLAYER) "§e$trackedPlayer§a's" else "§e${mode.name.lowercase()}§a"
        ChatUtils.chat("Started recording $label animation frames.")

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
        else -> gson.toJson(state.tracker.orderedFrames.toAnimationOutput())
    }

    private fun List<AnimationFrameTracker.FrameRecord>.toAnimationOutput() = AnimationOutput(
        textures = map { it.fullTexture },
        ticksPerTexture = map { it.serverTicks },
    )

    private fun buildPetOutput(state: RecordingState): String {
        val recordings = state.petRecordings.values
            .filter { it.tracker.frames.size > 1 }
            .associate { it.displayName to it.tracker.orderedFrames.toAnimationOutput() }
        val skinName = state.skinName
        return if (skinName != null) gson.toJson(mapOf(skinName to recordings))
        else gson.toJson(recordings)
    }
}
