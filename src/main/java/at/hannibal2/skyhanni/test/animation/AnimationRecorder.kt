package at.hannibal2.skyhanni.test.animation

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.minecraftevents.ClientEvents
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandLeaveEvent
import at.hannibal2.skyhanni.events.minecraft.ServerTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.DevApi
import at.hannibal2.skyhanni.test.animation.AnimationState.getFrameTexture
import at.hannibal2.skyhanni.test.animation.AnimationState.isPetTextureStand
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.EntityUtils.getPlayerEntities
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.MobUtils
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.animated.AnimatedItemStackRenderable.Companion.animatedItemStack
import at.hannibal2.skyhanni.utils.renderables.animated.framed.AnimatedFrameLocalStorage
import at.hannibal2.skyhanni.utils.renderables.animated.framed.FrameTickRateProvider
import at.hannibal2.skyhanni.utils.renderables.animated.framed.ItemStackAnimatedFrame
import at.hannibal2.skyhanni.utils.renderables.animated.rotate.AnimatedRotationLocalStorage
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.interactables.RotatableDragRenderable.Companion.rotatableDrag
import com.google.common.collect.ImmutableMultimap
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import com.mojang.authlib.properties.PropertyMap
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.ResolvableProfile
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@SkyHanniModule
object AnimationRecorder {

    private val config get() = DevApi.config.devTool.recordAnimations
    private val debugRenderables = mutableMapOf<String, Renderable>()
    private val rotationStorages = mutableMapOf<String, AnimatedRotationLocalStorage>()

    private data class RebuildRequest(
        val displayName: String,
        val mode: AnimationState.RecordingMode,
        val tracker: AnimationFrameTracker,
    )

    private val pendingRebuilds = ConcurrentHashMap<String, RebuildRequest>()

    // Entity IDs of armor stands that existed at the time of the last preview click.
    // These are pre-existing world pets and must not be recorded under the skin name.
    // Only newly appeared stands (the actual preview) should be tracked.
    private val preExistingStandIds = mutableSetOf<Int>()

    fun clearDebugRenderables() {
        pendingRebuilds.clear()
        debugRenderables.clear()
        rotationStorages.clear()
        preExistingStandIds.clear()
    }

    @HandleEvent
    fun onServerTick(event: ServerTickEvent) {
        if (!config.enabled.get()) return
        val currentState = AnimationState.state.takeIf {
            it.mode != AnimationState.RecordingMode.NONE
        } ?: return
        if (Minecraft.getInstance().level == null) return AnimationState.state.reset()
        event.recordTick(currentState)
    }

    private fun ServerTickEvent.recordTick(currentState: AnimationState.RecordingState) {
        when (currentState.mode) {
            AnimationState.RecordingMode.NONE -> return

            AnimationState.RecordingMode.HEAD -> {
                val frame = Minecraft.getInstance().player?.getItemBySlot(EquipmentSlot.HEAD)?.getFrameTexture()
                if (currentState.tracker.recordAndDidChange(tick, ClientEvents.totalTicks, frame)) {
                    rebuildRenderable("head", "head", AnimationState.RecordingMode.HEAD, currentState.tracker)
                }
            }

            AnimationState.RecordingMode.PET -> {
                val skinName = currentState.skinName ?: return
                val seenKeys = mutableSetOf<String>()
                EntityUtils.getEntitiesNearby<ArmorStand>(32.0) {
                    it.isPetTextureStand()
                }.forEach { stand ->
                    // Skip stands that existed before the preview click, those are world pets.
                    if (stand.id in preExistingStandIds) return@forEach
                    val displayName = stand.getPetDisplayName()
                    if (displayName == "Armor Stand") return@forEach

                    val recordingKey = "$skinName:$displayName"
                    seenKeys.add(recordingKey)
                    val recording = currentState.petRecordings.getOrPut(recordingKey) {
                        AnimationState.ArmorStandRecording(displayName, skinName)
                    }
                    val frame = stand.getItemBySlot(EquipmentSlot.MAINHAND).getFrameTexture()
                    if (recording.tracker.recordAndDidChange(tick, ClientEvents.totalTicks, frame)) {
                        rebuildRenderable(recordingKey, skinName, AnimationState.RecordingMode.PET, recording.tracker)
                    }
                }
                // Signal a sequence break for any tracked stand not seen this tick.
                // This prevents a spurious frame transition when the entity reappears after a reset.
                for ((key, recording) in currentState.petRecordings) {
                    if (key !in seenKeys) recording.tracker.record(tick, ClientEvents.totalTicks, null)
                }
            }

            AnimationState.RecordingMode.PLAYER -> {
                val frame = getPlayerEntities().firstOrNull {
                    it.name.string.equals(currentState.trackedPlayer, ignoreCase = true)
                }?.getItemBySlot(EquipmentSlot.HEAD)?.getFrameTexture()
                if (currentState.tracker.recordAndDidChange(tick, ClientEvents.totalTicks, frame)) {
                    rebuildRenderable(
                        currentState.trackedPlayer, currentState.trackedPlayer,
                        AnimationState.RecordingMode.PLAYER, currentState.tracker,
                    )
                }
            }
        }
    }

    @HandleEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!config.enabled.get()) return
        val item = event.item ?: return
        val lastLore = item.getLoreComponent().lastOrNull()?.string ?: return
        if (lastLore == "Right-click to preview!" && event.clickedButton != 1) return
        if (lastLore != "Right-click to preview!" && lastLore != "Click to preview!") return
        val displayName = item.cleanName()
        val internalName = item.getInternalNameOrNull()?.asString()
        if (internalName == "UPCOMING_SALE") return

        if (!AnimationState.isRecording) {
            AnimationState.startRecording(AnimationState.RecordingMode.PET)
            clearDebugRenderables()
            // Snapshot stands that currently exist so they can be excluded from recording.
            // The preview stand appears AFTER this click, so it will not be in this set.
            // On skin switches (already recording) we keep the original exclusion set.
            // The preview stand is still the same entity and must remain eligible for recording.
            preExistingStandIds.clear()
            EntityUtils.getEntitiesNearby<ArmorStand>(32.0) { it.isPetTextureStand() }
                .mapTo(preExistingStandIds) { it.id }
        }

        val isFire = displayName == "FIRE SALE!"
        val isNoColor = isFire || displayName.equals("UPCOMING SALE", ignoreCase = true)
        val skinName = AnimationState.state.apply {
            skinId = if (isFire) internalName else internalName ?: skinId
            skinColor = if (isNoColor) null else displayName
        }.skinName ?: return
        ChatUtils.chat("Skin identified: §e$skinName§a.")
    }

    @HandleEvent
    fun onIslandLeave(event: IslandLeaveEvent) {
        if (!AnimationState.isRecording) return
        ChatUtils.chat("§cRecording stopped: left island.")
        AnimationState.state.reset()
        clearDebugRenderables()
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) =
        AnimationRecorderCommand.handleEvent(event)

    @HandleEvent
    fun onWorldChange() {
        AnimationState.state.reset()
        clearDebugRenderables()
    }

    @HandleEvent(GuiRenderEvent::class)
    fun onGuiRender() {
        if (!config.enabled.get() || !config.debugOverlay.get()) return
        drainPendingRebuilds()
        if (debugRenderables.isEmpty()) return
        val combined = Renderable.vertical(spacing = 4) {
            addAll(debugRenderables.values)
        }
        config.debugPosition.renderRenderable(combined, "Record Animations Debug")
    }

    // Enqueues a rebuild request from a non-render thread (e.g. server tick).
    // No GL work is done here; building happens on the render thread in drainPendingRebuilds.
    private fun rebuildRenderable(
        key: String,
        displayName: String,
        mode: AnimationState.RecordingMode,
        tracker: AnimationFrameTracker,
    ) {
        if (!config.debugOverlay.get()) return
        if (tracker.orderedFrames.isEmpty() && tracker.previewFrames.isEmpty()) return
        pendingRebuilds[key] = RebuildRequest(displayName, mode, tracker)
    }

    // Must only be called from the render thread.
    private fun drainPendingRebuilds() {
        if (pendingRebuilds.isEmpty()) return
        val snapshot = pendingRebuilds.entries.toList()
        pendingRebuilds.clear()
        for ((key, request) in snapshot) {
            buildRenderable(key, request.displayName, request.mode, request.tracker)
        }
    }

    /**
     * Builds the debug renderable for a single recording key. Must be called on the render thread.
     *
     * @param key Unique key for this recording (display name for pets, player name, or "head").
     * @param displayName Human-readable label shown in the overlay header.
     * @param mode The current [AnimationState.RecordingMode].
     * @param tracker The [AnimationFrameTracker] whose frames drive the animated item and stats.
     */
    private fun buildRenderable(
        key: String,
        displayName: String,
        mode: AnimationState.RecordingMode,
        tracker: AnimationFrameTracker,
    ) {
        val frames = tracker.orderedFrames.takeIf { it.isNotEmpty() }
            ?: tracker.previewFrames.takeIf { it.isNotEmpty() }
            ?: return
        val rotStorage = rotationStorages.getOrPut(key) { AnimatedRotationLocalStorage() }
        val animFrames = frames.map { record ->
            ItemStackAnimatedFrame(record.toItemStack(), record.serverTicks)
        }
        val animatedItem = Renderable.animatedItemStack {
            frameStorage = AnimatedFrameLocalStorage(animFrames, FrameTickRateProvider.perFrame())
            rotationStorage = rotStorage
            scale = NeuItems.ITEM_FONT_SIZE * config.previewScale.get()
        }
        debugRenderables[key] = Renderable.vertical(spacing = 2) {
            addString("§a$displayName §7(${mode.name})")
            addString(tracker.captureStatsString)
            val detail = tracker.captureDetailString
            if (detail.isNotEmpty()) addString(detail)
            add(Renderable.rotatableDrag(animatedItem, rotStorage))
            addString(
                if (tracker.verificationErrors == 0) "§aNo errors"
                else "§c${tracker.verificationErrors} verification errors",
            )
        }
    }

    /**
     * Snapshots the tracker's observable state, records the frame, then returns true if anything
     * changed that would require a renderable rebuild (new frame learned, loop completed, or a
     * new raw frame added during learning).
     */
    private fun AnimationFrameTracker.recordAndDidChange(
        serverTick: Long,
        clientTick: Int,
        frame: AnimationFrameTracker.FrameRecord?,
    ): Boolean {
        val framesBefore = frames.size
        val loopsBefore = loopCount
        val rawBefore = rawFrameCount
        record(serverTick, clientTick, frame)
        return frames.size != framesBefore || loopCount != loopsBefore || rawFrameCount != rawBefore
    }

    /** Resolves the level-name display name of a pet armor stand by looking for a nearby [Lv N] label stand. */
    private fun ArmorStand.getPetDisplayName(): String =
        MobUtils.getArmorStandByRangeAll(this, 2.0)
            .firstOrNull { it.cleanName().startsWith("[Lv") }
            ?.name?.string ?: name.string

    private fun AnimationFrameTracker.FrameRecord.toItemStack(): ItemStack {
        val item = ItemStack(Items.PLAYER_HEAD)
        val id = uuid?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: UUID.randomUUID()
        val properties = ImmutableMultimap.builder<String, Property>()
            .put("textures", Property("textures", texture, signature))
            .build()
        val profile = GameProfile(id, "SkyHanniAnim", PropertyMap(properties))
        item.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile))
        return item
    }
}
