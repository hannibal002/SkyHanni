package at.hannibal2.skyhanni.features.dungeon.floor7

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.DungeonBossRoomEnterEvent
import at.hannibal2.skyhanni.events.DungeonCompleteEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.mixins.transformers.AccessorRendererLivingEntity
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HolographicEntities
import at.hannibal2.skyhanni.utils.HolographicEntities.HolographicEntity
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.getViewerPos
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.model.ModelPlayer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType
import net.minecraft.client.renderer.entity.RendererLivingEntity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11

@SkyHanniModule
object HolographicPlayerReplay {
    val storage get() = ProfileStorageData.playerSpecific?.dungeonGhost

    private var recording = false
    private var recordedPositions = mutableListOf<RecordedPosition>()

    private var currentRun: List<RecordedPosition>? = null

    private var recordedTime = SimpleTimeMark.farPast()

    private var playing = false
    private var playIndex = 0

    private val mc get() = Minecraft.getMinecraft()

    @HandleEvent
    fun onBossStart(event: DungeonBossRoomEnterEvent) {
        if (DungeonAPI.dungeonFloor?.contains("3") == false) return

        startRecording()
        if (storage?.bestRun?.isNotEmpty() == true) {
            currentRun = storage?.bestRun ?: run {
                ChatUtils.chat("null storage")
                return
            }
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
                storage?.bestTime = Long.MAX_VALUE
                storage?.bestRun = listOf()
                ChatUtils.chat("cleared!")
                return
            }
            strings.any { it.contains("play", ignoreCase = true) } -> {
                currentRun = storage?.bestRun ?: run {
                    ChatUtils.chat("null storage")
                    return
                }
                playIndex = 0
                playing = true
                ChatUtils.chat("playing")
                return
            }
            else -> {
                if (!recording) startRecording()
                else stopRecording()
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

    private fun stopRecording() {
        if (!recording) return
        ChatUtils.chat("stopped recording")
        recording = false
        attemptSave(recordedPositions, recordedTime.passedSince().inWholeMilliseconds)
        recordedPositions.clear()
        recordedTime = SimpleTimeMark.farPast()
    }

    private fun attemptSave(positions: List<RecordedPosition>, time: Long) {
        ChatUtils.chat("time: $time")
        ChatUtils.chat("pb: ${storage?.bestTime}")
        if (time < (storage?.bestTime ?: Long.MAX_VALUE)) {
            ChatUtils.chat("new pb!")
            storage?.bestTime = time
            storage?.bestRun = positions.map { it.copy() }
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (recording) {
            val player = mc.thePlayer
            val position = LorenzVec(player.posX, player.posY, player.posZ)
            val rotation = player.rotationYaw
            val pitch = player.rotationPitch
            val limbSwing = player.limbSwing
            val limbSwingAmount = player.limbSwingAmount
            val isSneaking = player.isSneaking
            val isRiding = player.isRiding
            val heldItem = player.heldItem
            val swingProgress = player.swingProgress

            recordedPositions.add(
                RecordedPosition(
                    position,
                    rotation,
                    pitch,
                    limbSwing,
                    limbSwingAmount,
                    isSneaking,
                    isRiding,
                    heldItem,
                    swingProgress,
                ),
            )
        }
        if (playing) {
            playIndex += 1
        }
    }

    @SubscribeEvent
    fun onRender(event: LorenzRenderWorldEvent) {
        if (!playing) return
        if (currentRun == null || currentRun?.size == 0) return
        val run = currentRun ?: return

        val fakePlayer = EntityOtherPlayerMP(null, mc.thePlayer.gameProfile)

        val index = playIndex.coerceIn(run.indices)
        val previousIndex = (playIndex - 1).coerceIn(run.indices)

        val recordedPosition = run[index]
        val previousPosition = run[previousIndex]

        val interpolatedData = interpolateRecordedPosition(previousPosition, recordedPosition, event.partialTicks)

        val holographicPlayer = HolographicEntities.HolographicBase(fakePlayer)

        val instance = holographicPlayer.instance(
            interpolatedData.position,
            -interpolatedData.yaw + 360f,
        )

        newRenderHolographicEntity(
            instance,
            event.partialTicks,
            interpolatedData,
            fakePlayer,
        )
    }

    private fun interpolateRecordedPosition(last: RecordedPosition, next: RecordedPosition, progress: Float): RecordedPosition {
        val interpolatedPosition = interpolatePosition(last.position, next.position, progress)
        val interpolatedYaw = interpolateRotation(last.yaw, next.yaw, progress)
        val interpolatedPitch = interpolateRotation(last.pitch, next.pitch, progress)
        val interpolatedLimbSwing = interpolateValue(last.limbSwing, next.limbSwing, progress)
        val interpolatedLimbSwingAmount = interpolateValue(last.limbSwingAmount, next.limbSwingAmount, progress)
        val interpolatedSwingProgress = interpolateValue(last.swingProgress, next.swingProgress, progress)
        return RecordedPosition(
            interpolatedPosition,
            -interpolatedYaw,
            interpolatedPitch,
            interpolatedLimbSwing,
            interpolatedLimbSwingAmount,
            last.sneaking,
            last.isRiding,
            last.heldItem,
            interpolatedSwingProgress,
        )
    }

    private fun interpolateValue(last: Float, next: Float, progress: Float): Float {
        return last + (next - last) * progress
    }

    private fun interpolatePosition(last: LorenzVec, next: LorenzVec, progress: Float): LorenzVec {
        val x = last.x + (next.x - last.x) * progress
        val y = last.y + (next.y - last.y) * progress
        val z = last.z + (next.z - last.z) * progress
        return LorenzVec(x, y, z)
    }

    private fun interpolateRotation(last: Float, next: Float, progress: Float): Float {
        var direction: Float = next - last
        while (direction < -180.0f) {
            direction += 360.0f
        }
        while (direction >= 180.0f) {
            direction -= 360.0f
        }
        return last + progress * direction
    }

    private fun <T : EntityLivingBase> newRenderHolographicEntity(
        holographicEntity: HolographicEntity<T>,
        partialTicks: Float,
        recordedPosition: RecordedPosition,
        fakePlayer: EntityOtherPlayerMP,
    ) {
        val renderManager = mc.renderManager
        val renderer = renderManager.getEntityRenderObject<EntityLivingBase>(fakePlayer)
        renderer as RendererLivingEntity<T>
        renderer as AccessorRendererLivingEntity<T>

        if (renderer.mainModel !is ModelPlayer) return
        val newModel = renderer.mainModel as ModelPlayer

        renderer.setRenderOutlines(false)
        if (!renderer.bindEntityTexture_skyhanni(holographicEntity.entity))
            return

        GlStateManager.pushMatrix()
        val viewerPosition = getViewerPos(partialTicks)
        val mobPosition = holographicEntity.interpolatedPosition(partialTicks)
        val renderingOffset = mobPosition - viewerPosition
        renderingOffset.applyTranslationToGL()
        GlStateManager.enableRescaleNormal()
        GlStateManager.scale(-1f, -1f, 1f)
        GlStateManager.translate(0F, -1.5078125f, 0f)
        val ageInTicks: Float = 1_000_000.toFloat()
        val scaleFactor = 0.0625f
        val netHeadYaw: Float = holographicEntity.interpolatedYaw(partialTicks)
        renderer.setBrightness_skyhanni(holographicEntity.entity, 0f, true)
        GlStateManager.color(1.0f, 1.0f, 1.0f, 0.6f)
        GlStateManager.depthMask(true)
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.alphaFunc(GL11.GL_GREATER, 1 / 255F)

        GlStateManager.enableTexture2D()

        GlStateManager.rotate(netHeadYaw + 180f, 0f, 1f, 0f) //correct looking fowards
        renderer.mainModel.isChild = false //check if skytils small people is active?
        renderer.mainModel.isRiding = recordedPosition.isRiding

        val offset = 0.1f
        GlStateManager.translate(0f, offset, 0f)

        newModel.isSneak = recordedPosition.sneaking
        newModel.heldItemRight = if (recordedPosition.heldItem == null) 0 else 1
        newModel.swingProgress = recordedPosition.swingProgress
        newModel.render(
            fakePlayer,
            recordedPosition.limbSwing,
            recordedPosition.limbSwingAmount,
            ageInTicks,
            0f,
            recordedPosition.pitch,
            scaleFactor,
        )

        if (recordedPosition.heldItem != null) {
            GlStateManager.pushMatrix()
            GlStateManager.depthMask(true)
            GlStateManager.color(1.0f, 1.0f, 1.0f, 0.6f)
            GlStateManager.translate(-0.4f, 0.5f, 0f)
            mc.renderItem.renderItem(recordedPosition.heldItem, TransformType.THIRD_PERSON)
            GlStateManager.popMatrix()
        }


        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f)
        GlStateManager.color(1f, 1f, 1f, 1f)
        GlStateManager.depthMask(true)
        GlStateManager.disableBlend()
        GlStateManager.popMatrix()
    }
}

data class RecordedPosition(
    val position: LorenzVec,
    val yaw: Float,
    val pitch: Float,
    val limbSwing: Float,
    val limbSwingAmount: Float,
    val sneaking: Boolean,
    val isRiding: Boolean,
    val heldItem: ItemStack?,
    val swingProgress: Float,
)
