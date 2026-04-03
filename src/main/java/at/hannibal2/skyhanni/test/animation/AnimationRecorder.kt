package at.hannibal2.skyhanni.test.animation

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.minecraftevents.ClientEvents
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
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

@SkyHanniModule
object AnimationRecorder {

    private val config get() = DevApi.config.devTool.recordAnimations
    private val debugRenderables = mutableMapOf<String, Renderable>()
    private val rotationStorages = mutableMapOf<String, AnimatedRotationLocalStorage>()

    fun clearDebugRenderables() {
        debugRenderables.clear()
        rotationStorages.clear()
    }

    @HandleEvent
    fun onServerTick(event: ServerTickEvent) {
        if (!config.enabled.get()) return
        val serverTick = event.tick
        val clientTick = ClientEvents.totalTicks
        val current = AnimationState.state
            .takeIf { it.mode != AnimationState.RecordingMode.NONE } ?: return
        if (Minecraft.getInstance().level == null) return AnimationState.state.reset()

        when (current.mode) {
            AnimationState.RecordingMode.NONE -> return

            AnimationState.RecordingMode.HEAD -> {
                val frame = Minecraft.getInstance().player
                    ?.getItemBySlot(EquipmentSlot.HEAD)?.getFrameTexture()
                if (current.tracker.record(serverTick, clientTick, frame)) {
                    rebuildRenderable("head", "head", AnimationState.RecordingMode.HEAD, current.tracker)
                }
            }

            AnimationState.RecordingMode.PET -> EntityUtils.getEntitiesNearby<ArmorStand>(32.0) {
                it.isPetTextureStand()
            }.forEach { stand ->
                val displayName = MobUtils.getArmorStandByRangeAll(stand, 2.0)
                    .firstOrNull { it.cleanName().startsWith("[Lv") }
                    ?.name?.string ?: stand.name.string
                if (displayName == "Armor Stand") return@forEach
                val recording = current.petRecordings.getOrPut(displayName) {
                    AnimationState.ArmorStandRecording(displayName)
                }
                stand.getItemBySlot(EquipmentSlot.MAINHAND).getFrameTexture()?.let { frame ->
                    if (recording.tracker.record(serverTick, clientTick, frame)) {
                        rebuildRenderable(
                            displayName, displayName,
                            AnimationState.RecordingMode.PET, recording.tracker,
                        )
                    }
                }
            }

            AnimationState.RecordingMode.PLAYER -> {
                val frame = getPlayerEntities()
                    .firstOrNull { it.name.string.equals(current.trackedPlayer, ignoreCase = true) }
                    ?.getItemBySlot(EquipmentSlot.HEAD)?.getFrameTexture()
                if (current.tracker.record(serverTick, clientTick, frame)) {
                    rebuildRenderable(
                        current.trackedPlayer, current.trackedPlayer,
                        AnimationState.RecordingMode.PLAYER, current.tracker,
                    )
                }
            }
        }
    }

    @HandleEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!config.enabled.get()) return
        if (!AnimationState.isRecording) return
        val item = event.item ?: return
        val lastLore = item.getLoreComponent().lastOrNull()?.string ?: return
        if (lastLore != "Right-click to preview!" && lastLore != "Click to preview!") return
        val displayName = item.cleanName()
        val internalName = item.getInternalNameOrNull()?.asString()
        if (displayName == "FIRE SALE!") {
            AnimationState.state.skinColor = null
            AnimationState.state.skinId = internalName
        } else {
            AnimationState.state.skinId = internalName ?: AnimationState.state.skinId
            AnimationState.state.skinColor = displayName
        }
        val skinName = AnimationState.state.skinName ?: return
        ChatUtils.chat("Skin identified: §e$skinName§a.")
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
        if (debugRenderables.isEmpty()) return
        val combined = Renderable.vertical(spacing = 4) {
            addAll(debugRenderables.values)
        }
        config.debugPosition.renderRenderable(combined, "Record Animations Debug")
    }

    /**
     * Rebuilds the debug renderable for a single recording key when a new loop completes.
     *
     * @param key Unique key for this recording (display name for pets, player name, or "head").
     * @param displayName Human-readable label shown in the overlay header.
     * @param mode The current [AnimationState.RecordingMode].
     * @param tracker The [AnimationFrameTracker] whose frames drive the animated item and stats.
     */
    private fun rebuildRenderable(
        key: String,
        displayName: String,
        mode: AnimationState.RecordingMode,
        tracker: AnimationFrameTracker,
    ) {
        if (!config.debugOverlay.get()) return
        val frames = tracker.orderedFrames.takeIf { it.isNotEmpty() } ?: return

        val rotStorage = rotationStorages.getOrPut(key) { AnimatedRotationLocalStorage() }

        val animFrames = frames.map { record ->
            ItemStackAnimatedFrame(record.toItemStack(), record.serverTicks)
        }

        val animatedItem = Renderable.animatedItemStack {
            frameStorage = AnimatedFrameLocalStorage(animFrames, FrameTickRateProvider.perFrame())
            rotationStorage = rotStorage
        }

        debugRenderables[key] = Renderable.vertical(spacing = 2) {
            addString("§a$displayName §7(${mode.name})")
            addString(tracker.captureStatsString)
            add(Renderable.rotatableDrag(animatedItem, rotStorage))
            addString(tracker.verificationStatusString)
        }
    }

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
