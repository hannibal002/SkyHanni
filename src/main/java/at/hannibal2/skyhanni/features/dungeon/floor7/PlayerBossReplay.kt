package at.hannibal2.skyhanni.features.dungeon.floor7

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.events.DungeonBossRoomEnterEvent
import at.hannibal2.skyhanni.events.DungeonCompleteEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.isEnchanted
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import com.mojang.authlib.GameProfile
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.UUID

@SkyHanniModule
object PlayerBossReplay {
    private var recording = false
    private var recordedPositions = mutableListOf<RecordedPosition>()

    private var currentRun: List<RecordedPosition>? = null

    private var recordedTime = SimpleTimeMark.farPast()

    private var playing = false
    private var playIndex = 0

    private var bestRun: List<RecordedPosition> = listOf()
    private var bestRunTime: Long = Long.MAX_VALUE

    private val player get() = Minecraft.getMinecraft().thePlayer


    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (recording) {
            val player = player
            val position = LorenzVec(player.posX, player.posY, player.posZ)
            val yaw = player.rotationYaw
            val pitch = player.rotationPitch
            val limbSwing = player.limbSwing
            val limbSwingAmount = player.limbSwingAmount
            val isSneaking = player.isSneaking
            val isRiding = player.isRiding
            val heldItem = player.heldItem
            val swingProgress = player.swingProgress
            val isUsingItem = player.itemInUseCount > 0
            val isEating = player.isEating
            player.isEating

            recordedPositions.add(
                RecordedPosition(
                    position,
                    yaw,
                    pitch,
                    limbSwing,
                    limbSwingAmount,
                    swingProgress,
                    heldItem.getInternalName(),
                    heldItem.isEnchanted(),
                    isUsingItem,
                    isEating,
                    isSneaking,
                    isRiding
                ),
            )
        }
        if (playing) {
            playIndex += 1
            if (playIndex > (currentRun?.size ?: 0)) {
                playIndex = 0
            }
        }
    }

    @HandleEvent
    fun onBossStart(event: DungeonBossRoomEnterEvent) {
        if (DungeonAPI.dungeonFloor?.contains("3") == false) return

        startRecording()
        if (bestRun.isNotEmpty()) {
            currentRun = bestRun
            playIndex = 0
            playing = true
        }
    }

    @SubscribeEvent
    fun onBossEnd(event: DungeonCompleteEvent) {
        if (DungeonAPI.dungeonFloor?.contains("3") == false) return

        stopRecording()
        if (playing) {
            playing = false
            playIndex = 0
        }
    }

    fun command(strings: Array<String>) {
        when {
            strings.any { it.contains("clear", ignoreCase = true) } -> {
                recording = false
                recordedPositions.clear()
                recordedTime = SimpleTimeMark.farPast()
                bestRunTime = Long.MAX_VALUE
                bestRun = listOf()
                ChatUtils.chat("cleared!")
                return
            }
            strings.any { it.contains("play", ignoreCase = true) } -> {
                currentRun = bestRun
                playIndex = 0
                playing = true
                ChatUtils.chat("playing")
                return
            }
            else -> {
                if (!recording) startRecording()
                else stopRecording(true)
                return
            }
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
        attemptSave(recordedPositions, recordedTime.passedSince().inWholeMilliseconds, if (isManual) "manual" else DungeonAPI.dungeonFloor)
        recordedPositions.clear()
        recordedTime = SimpleTimeMark.farPast()
    }

    private fun attemptSave(positions: List<RecordedPosition>, time: Long, type: String?) {
        ChatUtils.chat("time: $time")
        ChatUtils.chat("pb: $bestRunTime")
        ChatUtils.chat("position size: ${positions.size}")
        if (time < bestRunTime) {
            ChatUtils.chat("new pb! trying to save to '$type'")
            when (type) {
                "manual" -> {
                    ChatUtils.chat("manual save")
                    OSUtils.copyToClipboard(DungeonGhostData(positions, time, player.gameProfile).toString())
                    SkyHanniMod.dungeonReplayData.manual = DungeonGhostData(positions, time, player.gameProfile)
                }
                "F3" -> {
                    SkyHanniMod.dungeonReplayData.floor3 = DungeonGhostData(positions, time, player.gameProfile)
                }
                "F7" -> {
                    SkyHanniMod.dungeonReplayData.floor7 = DungeonGhostData(positions, time, player.gameProfile)
                }
                "M7" -> {
                    SkyHanniMod.dungeonReplayData.floorMaster7 = DungeonGhostData(positions, time, player.gameProfile)
                }
            }
            SkyHanniMod.configManager.saveConfig(ConfigFileType.DUNGEON_REPLAY, "Updated Dungeon Replays")
            bestRunTime = time
            bestRun = positions.map { it.copy() }
        }
    }

    @SubscribeEvent
    fun onRender(event: LorenzRenderWorldEvent) {
        if (!playing) return
        if (currentRun == null) return
        currentRun.let {
            if (it != null) {
                HolographicPlayerReplay.renderHolographicPlayer(event, it, player.gameProfile, playIndex)
            }
        }
    }
}

data class DungeonGhostData(
    val recordedPositions: List<RecordedPosition> = listOf(),
    val time: Long = Long.MAX_VALUE,
    val gameProfile: GameProfile = GameProfile(UUID.fromString("49f4c15d-14e0-4d75-be1b-9c1b85bad53c"), "martimavocado")
)
