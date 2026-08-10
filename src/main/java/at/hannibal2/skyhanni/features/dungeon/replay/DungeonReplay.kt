package at.hannibal2.skyhanni.features.dungeon.replay

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.config.features.dungeon.replay.DungeonBossReplayConfig.DungeonFloorWithBoss
import at.hannibal2.skyhanni.events.dungeon.DungeonBossRoomEnterEvent
import at.hannibal2.skyhanni.events.dungeon.DungeonCompleteEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HolographicEntities
import at.hannibal2.skyhanni.utils.HolographicEntities.renderHolographicEntity
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.toLorenzVec
import com.google.gson.annotations.Expose
import com.mojang.authlib.GameProfile
import net.minecraft.client.player.AbstractClientPlayer
import java.util.UUID

@SkyHanniModule
object DungeonReplay {
    private val config get() = SkyHanniMod.feature.dungeon.bossReplay
    private val storage get() = SkyHanniMod.dungeonReplayStorage

    private var recording: RecordingData? = null
    private var playing: PlayingData? = null

    private var holographicPlayer: HolographicEntities.HolographicEntity<AbstractClientPlayer>? = null

    private val currentFloor: DungeonFloorWithBoss?
        get() {
            val floorName = DungeonApi.dungeonFloor ?: return null
            return DungeonFloorWithBoss.findByStringOrNull(floorName)
        }

    @HandleEvent
    private fun onTick(event: SkyHanniTickEvent) {
        if (!config.enabled) return
        if (!DungeonApi.inDungeon()) {
            playing = null
            return
        }

        playing?.let {
            if (it.currentTick < it.replay.recordedPositions.size) {
                it.currentTick += 1
            }
        }

        recording?.let {
            saveCurrentTick()
        }
    }

    private fun saveCurrentTick() {
        val player = MinecraftCompat.localPlayerOrNull ?: return
        val recording = recording ?: return

        val position = player.position().toLorenzVec()
        val yaw = player.rotationVector.y
        val heldItemID = player.activeItem.getInternalNameOrNull()

        val previousPosition = RecordedPositionDelta.getComplete(recording.positionList, recording.positionList.size - 1)
        val newPosition = RecordedPositionDelta(
            position.takeIf { it != previousPosition.position },
            yaw.takeIf { it != previousPosition.yaw },
            heldItemID.takeIf { it != previousPosition.heldItemID },
        )
        recording.positionList.add(newPosition)
    }

    @HandleEvent
    private fun onBossStart(event: DungeonBossRoomEnterEvent) {
        if (!config.enabled) return
        if (currentFloor?.isEnabled() != true) return

        startRecording()

        storage.replays[currentFloor]?.let {
            startPlaying(it)
        }
    }

    @HandleEvent
    private fun onBossEnd(event: DungeonCompleteEvent) {
        // this doesn't check for config or active floor so a previously started recording can safely end
        if (recording == null) return

        stopRecording()
        playing = null
    }

    private fun startPlaying(data: DungeonGhostData) {
        if (data.recordedPositions.isEmpty() || data.time == 0L) return

        holographicPlayer = HolographicEntities.createPlayerHologram(
            data.recordedPositions.first().position ?: run {
                ErrorManager.skyHanniError("first pos is null")
            },
            data.recordedPositions.first().yaw ?: run {
                ErrorManager.skyHanniError("first yaw is null")
            },
            GameProfile(data.playerUUID, data.playerName)
        ) ?: run {
            ErrorManager.skyHanniError("null hologram")
        }

        playing = PlayingData(data, holographicPlayer ?: return)
    }

    private fun startRecording() {
        if (recording != null) return
        ChatUtils.chat("start recording")

        recording = RecordingData(currentFloor ?: return)
    }

    private fun stopRecording() {
        val recording = recording ?: return
        val player = MinecraftCompat.localPlayerOrNull ?: return

        ChatUtils.chat("stopped recording")
        val ghostData = DungeonGhostData(
            recording.positionList,
            recording.startTime.passedSince().inWholeMilliseconds,
            player.gameProfile.id, player.gameProfile.name
        )
        val currentPB = storage.replays[recording.targetDungeon]

        if (currentPB == null || ghostData.time > currentPB.time) {
            ChatUtils.chat("new pb!")

            storage.replays[recording.targetDungeon] = ghostData
            SkyHanniMod.configManager.saveConfig(ConfigFileType.DUNGEON_REPLAY, "Updated Dungeon Replays")
        }

        DungeonReplay.recording = null
    }

    @HandleEvent
    private fun onRender(event: SkyHanniRenderWorldEvent) {
        val playData = playing ?: return
        val recordedPosition = RecordedPositionDelta.getComplete(
            playData.replay.recordedPositions,
            playData.currentTick
        )

        event.renderHolographicEntity(
            holographicPlayer ?: return,
            config.opacity / 100f,
            recordedPosition.heldItemID?.getItemStackOrNull() ?: SafeItemStack.EMPTY
        )
    }

    data class DungeonGhostData(
        @Expose val recordedPositions: MutableList<RecordedPositionDelta> = mutableListOf(),
        @Expose val time: Long = Long.MAX_VALUE,
        @Expose val playerUUID: UUID = UUID.fromString("49f4c15d-14e0-4d75-be1b-9c1b85bad53c"),
        @Expose val playerName: String = "martimavocado",
    )

    data class RecordingData(val targetDungeon: DungeonFloorWithBoss) {
        val startTime = SimpleTimeMark.now()
        val positionList = mutableListOf<RecordedPositionDelta>()
    }

    data class PlayingData(
        val replay: DungeonGhostData,
        val holographicPlayer: HolographicEntities.HolographicEntity<AbstractClientPlayer>,
    ) {
        var currentTick = 0
            set(value) {
                field = value.coerceIn(0, replay.recordedPositions.size)

                val currentTickData = RecordedPositionDelta.getComplete(replay.recordedPositions, field)

                holographicPlayer.moveTo(currentTickData.position, currentTickData.yaw, false)
            }
    }

    data class RecordedPosition(
        val position: LorenzVec,
        val yaw: Float,
        val heldItemID: NeuInternalName? = null,
    )

    data class RecordedPositionDelta(
        @Expose val position: LorenzVec? = null,
        @Expose val yaw: Float? = null,
        @Expose val heldItemID: NeuInternalName? = null,
    ) {
        companion object {
            fun getComplete(positions: List<RecordedPositionDelta>, index: Int): RecordedPosition {
                var incompletePositions = RecordedPositionDelta()

                for (i in index downTo 0) {
                    if (i >= positions.size) continue
                    val position = positions[i]

                    incompletePositions = incompletePositions.copy(
                        position = incompletePositions.position ?: position.position,
                        yaw = incompletePositions.yaw ?: position.yaw,
                        heldItemID = incompletePositions.heldItemID ?: position.heldItemID,
                    )

                    if (incompletePositions.isComplete()) break
                }

                return RecordedPosition(
                    incompletePositions.position ?: LorenzVec(),
                    incompletePositions.yaw ?: 0f,
                    incompletePositions.heldItemID,
                )
            }

            private fun RecordedPositionDelta.isComplete(): Boolean {
                return position != null && yaw != null && heldItemID != null
            }
        }
    }
}
