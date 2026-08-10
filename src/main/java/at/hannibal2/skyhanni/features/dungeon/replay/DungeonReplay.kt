package at.hannibal2.skyhanni.features.dungeon.replay

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.dungeon.DungeonBossRoomEnterEvent
import at.hannibal2.skyhanni.events.dungeon.DungeonCompleteEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.features.dungeon.replay.HolographicPlayerReplay.renderHolographicEntity
import at.hannibal2.skyhanni.features.dungeon.replay.Vector2.Companion.toVector2
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HolographicEntities
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.toLorenzVec
import com.mojang.authlib.GameProfile
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.world.InteractionHand

@SkyHanniModule
object DungeonReplay {
    private var recording = false
    private var recordedPositions = mutableListOf<RecordedPositionDelta>()

    private var recordedTime = SimpleTimeMark.farPast()

    private var holographicPlayer: HolographicEntities.HolographicEntity<AbstractClientPlayer>? = null

    private var playing = false
    private var playIndex = 0

    private var currentRun: DungeonGhostData? = null
    private var bestRun = DungeonGhostData()

    private val storage get() = SkyHanniMod.dungeonReplayStorage

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        val player = MinecraftCompat.localPlayerOrNull ?: return

        if (playing) {
            if (playIndex >= (currentRun?.recordedPositions?.size ?: 0)) {
                playIndex = 0
            } else {
                playIndex += 1
            }
        }

        if (recording) {
            val position = player.position().toLorenzVec()
            val rotation = player.rotationVector.toVector2()
            val limbSwing = player.attackAnim
            val isCrouching = player.isCrouching
            val isRiding = player.isPassenger
            val mainHand = player.swingingArm ?: InteractionHand.MAIN_HAND
            val heldItem = player.activeItem.getInternalNameOrNull()
            val itemEnchanted = player.activeItem.isEnchanted
            val isUsingItem = player.isUsingItem

            val previousPosition = RecordedPositionDelta.getComplete(recordedPositions, recordedPositions.size - 1)
            val newPosition = RecordedPositionDelta(
                position.takeIf { it != previousPosition.position },
                rotation.takeIf { it != previousPosition.rotation },
                limbSwing.takeIf { it != previousPosition.limbSwing },
                isCrouching.takeIf { it != previousPosition.isCrouching },
                isRiding.takeIf { it != previousPosition.isRiding },
                mainHand.takeIf { it != previousPosition.interactionHand },
                heldItem.takeIf { it != previousPosition.heldItemID },
                itemEnchanted.takeIf { it != previousPosition.itemEnchanted },
                isUsingItem.takeIf { it != previousPosition.isUsingItem },
            )
            recordedPositions.add(newPosition)
        }
    }

    @HandleEvent
    fun onBossStart(event: DungeonBossRoomEnterEvent) {
        if (DungeonApi.dungeonFloor?.contains("3") == false) return

        startRecording()
        if (bestRun.recordedPositions.isNotEmpty()) {
            startPlaying(bestRun)
        }
    }

    @HandleEvent
    fun onBossEnd(event: DungeonCompleteEvent) {
        if (DungeonApi.dungeonFloor?.contains("3") == false) return

        stopRecording()
        if (playing) {
            playing = false
            playIndex = 0
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shrecord") {
            description = "dungeon replay"
            category = CommandCategory.USERS_ACTIVE

            literal("clear") {
                simpleCallback {
                    recording = false
                    recordedPositions.clear()
                    recordedTime = SimpleTimeMark.farPast()
                    bestRun = DungeonGhostData()
                    ChatUtils.chat("cleared!")
                }
            }

            literal("play") {
                simpleCallback {
                    startPlaying(bestRun)
                    ChatUtils.chat("playing")
                }
            }

            literal("storage") {
                simpleCallback {
                    println(storage.manual)
                    ChatUtils.chat(storage.manual.toString())
                    startPlaying(storage.manual)
                    ChatUtils.chat("playing from storage")
                }
            }

            literal("stop") {
                simpleCallback {
                    playing = false
                    ChatUtils.chat("stopped")
                }
            }

            simpleCallback {
                if (!recording) startRecording()
                else stopRecording(true)
            }
        }
    }

    private fun startPlaying(data: DungeonGhostData) {
        if (data.recordedPositions.isEmpty()) {
            ChatUtils.chat("empty replay list")
            return
        }

        currentRun = data
        playing = true
        playIndex = 0

        holographicPlayer = HolographicEntities.createPlayerHologram(
            data.recordedPositions.first().position ?: run {
                ErrorManager.skyHanniError("first pos is null")
            },
            data.recordedPositions.first().rotation?.y ?: run {
                ErrorManager.skyHanniError("first rotation is null")
            },
            GameProfile(data.playerUUID, data.playerName)
        ) ?: run {
            ErrorManager.skyHanniError("null hologram")
        }
    }

    private fun startRecording() {
        if (recording) return
        ChatUtils.chat("recording")
        recordedPositions.clear()
        recordedTime = SimpleTimeMark.now()
        recording = true
    }

    private fun stopRecording(isManual: Boolean = false) {
        if (!recording) return
        ChatUtils.chat("stopped recording")
        recording = false
        attemptSave(recordedPositions, recordedTime.passedSince().inWholeMilliseconds, if (isManual) "manual" else DungeonApi.dungeonFloor)
        recordedPositions.clear()
        recordedTime = SimpleTimeMark.farPast()
    }

    private fun attemptSave(positions: List<RecordedPositionDelta>, time: Long, type: String?) {
        val player = MinecraftCompat.localPlayerOrNull ?: return
        ChatUtils.chat("time: $time")
        ChatUtils.chat("pb: ${bestRun.time}")
        ChatUtils.chat("position size: ${positions.size}")


        if (time < bestRun.time) {
            ChatUtils.chat("new pb! trying to save to '$type'")
            val ghostData = DungeonGhostData(positions.toMutableList(), time, player.gameProfile.id, player.gameProfile.name)
            when (type) {
                "manual" -> {
                    storage.manual = ghostData
                    println(storage.manual)
                }

                "F3" -> {
                    storage.floor3 = ghostData
                }

                "F7" -> {
                    storage.floor7 = ghostData
                }

                "M7" -> {
                    storage.floorMaster7 = ghostData
                }
            }
            SkyHanniMod.configManager.saveConfig(ConfigFileType.DUNGEON_REPLAY, "Updated Dungeon Replays")
            bestRun = bestRun.copy()
        }
    }

    @HandleEvent
    fun onRender(event: SkyHanniRenderWorldEvent) {
        if (!playing) return
        if (currentRun == null) return

        val previousIndex = if (playIndex == 0) 0 else playIndex - 1

        val recordedPosition = RecordedPositionDelta.getComplete(
            currentRun?.recordedPositions ?: listOf(),
            playIndex
        )
        val previousPosition = RecordedPositionDelta.getComplete(currentRun?.recordedPositions ?: listOf(), previousIndex)

        event.renderHolographicEntity(
            holographicPlayer ?: return,
            0.3f,
            recordedPosition,
            previousPosition,
            playIndex
        )
    }
}
