package at.hannibal2.skyhanni.features.dungeon.floor7

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketSentEvent
import at.hannibal2.skyhanni.mixins.transformers.AccessorRendererLivingEntity
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.HolographicEntities
import at.hannibal2.skyhanni.utils.HolographicEntities.HolographicEntity
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.RenderUtils.getViewerPos
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.entity.RendererLivingEntity
import net.minecraft.entity.EntityLivingBase
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11

@SkyHanniModule
object TerminalGhosts { //TODO: figure out sneaking
    private var recording = false

    private val recordedPositions = mutableListOf<RecordedPosition>()

    fun command(args: Array<String>) {
        if (args.isEmpty()) return
        when (args[0]) {
            "true" -> startRecording()
            "clear" -> recordedPositions.clear()
            else -> stopRecording()
        }
    }

    private fun startRecording() {
        if (recording) return
        recordedPositions.clear()
        recording = true
    }

    private fun stopRecording() {
        if (!recording) return
        recording = false
    }

    @HandleEvent
    fun onPacket(event: PacketSentEvent) {
        if (!recording) return

        val player = Minecraft.getMinecraft().thePlayer
        val position = LorenzVec(player.posX, player.posY, player.posZ)
        val rotation = player.rotationYaw
        val pitch = player.rotationPitch
        val limbSwing = player.limbSwing
        val limbSwingAmount = player.limbSwingAmount
        val isSneaking = player.isSneaking

        recordedPositions.add(
            RecordedPosition(
                position,
                rotation,
                pitch,
                limbSwing,
                limbSwingAmount,
                isSneaking
            )
        )
    }

    data class RecordedPosition(
        val position: LorenzVec,
        val yaw: Float,
        val pitch: Float,
        val limbSwing: Float,
        val limbSwingAmount: Float,
        val sneaking: Boolean
    )

    @SubscribeEvent
    fun onRender(event: LorenzRenderWorldEvent) {
        if (recording || recordedPositions.isEmpty()) return

        val realPlayer = Minecraft.getMinecraft().thePlayer
        val fakePlayer = HolographicEntities.HolographicBase(EntityOtherPlayerMP(null, realPlayer.gameProfile))

        val frameIndex = (Minecraft.getMinecraft().theWorld.totalWorldTime % recordedPositions.size).toInt()
        val recordedPosition = recordedPositions[frameIndex]
        val previousFrameIndex = if (frameIndex == 0) recordedPositions.size - 1 else frameIndex - 1
        val previousPosition = recordedPositions[previousFrameIndex]

        val interpolatedData = interpolateRecordedPosition(previousPosition, recordedPosition, event.partialTicks)

        val instance = fakePlayer.instance(
            interpolatedData.position,
            -interpolatedData.yaw + 360f
        )

        event.drawString(interpolatedData.position.add(y = 2.35), "sneaking: ${recordedPosition.sneaking}")
        newRenderHolographicEntity(
            instance,
            event.partialTicks,
            interpolatedData.limbSwing,
            interpolatedData.limbSwingAmount,
            interpolatedData.pitch
        )
    }

    private fun interpolateRecordedPosition(last: RecordedPosition, next: RecordedPosition, progress: Float): RecordedPosition {
        val interpolatedPosition = interpolatePosition(last.position, next.position, progress)
        val interpolatedYaw = interpolateRotation(last.yaw, next.yaw, progress)
        val interpolatedPitch = interpolateRotation(last.pitch, next.pitch, progress)
        val interpolatedLimbSwing = interpolateValue(last.limbSwing, next.limbSwing, progress)
        val interpolatedLimbSwingAmount = interpolateValue(last.limbSwingAmount, next.limbSwingAmount, progress)
        return RecordedPosition(
            interpolatedPosition,
            -interpolatedYaw,
            interpolatedPitch,
            interpolatedLimbSwing,
            interpolatedLimbSwingAmount,
            last.sneaking
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
        limbSwing: Float = 0F,
        limbSwingAmount: Float = 0F,
        headPitch: Float = 0F
    ) {
        val renderManager = Minecraft.getMinecraft().renderManager
        val renderer = renderManager.getEntityRenderObject<EntityLivingBase>(holographicEntity.entity)
        renderer as RendererLivingEntity<T>
        renderer as AccessorRendererLivingEntity<T>

        renderer.setRenderOutlines(false)
        if (!renderer.bindEntityTexture_skyhanni(holographicEntity.entity))
            return

        GlStateManager.pushMatrix()
        val viewerPosition = getViewerPos(partialTicks)
        val mobPosition = holographicEntity.interpolatedPosition(partialTicks)
        val renderingOffset = mobPosition - viewerPosition
        renderingOffset.applyTranslationToGL()
        GlStateManager.disableCull()
        GlStateManager.enableRescaleNormal()
        GlStateManager.scale(-1f, -1f, 1f)
        GlStateManager.translate(0F, -1.5078125f, 0f)
        val ageInTicks: Float = 1_000_000.toFloat()
        val scaleFactor = 0.0625f
        val netHeadYaw: Float = holographicEntity.interpolatedYaw(partialTicks)
        renderer.setBrightness_skyhanni(holographicEntity.entity, 0f, true)
        GlStateManager.color(1.0f, 1.0f, 1.0f, 0.4f)
        GlStateManager.depthMask(false)
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.alphaFunc(GL11.GL_GREATER, 1 / 255F)

        GlStateManager.enableTexture2D()
        GlStateManager.rotate(netHeadYaw, 0f, 1f, 0f) //correct looking fowards
        renderer.mainModel.render(
            holographicEntity.entity,
            limbSwing,
            limbSwingAmount,
            ageInTicks,
            180f,
            headPitch,
            scaleFactor
        )
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f)
        GlStateManager.color(1f, 1f, 1f, 1f)
        GlStateManager.depthMask(true)
        GlStateManager.disableBlend()
        GlStateManager.popMatrix()
    }
}
