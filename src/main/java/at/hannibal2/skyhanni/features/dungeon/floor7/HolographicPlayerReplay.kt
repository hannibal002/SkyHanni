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
import net.minecraft.client.renderer.EntityRenderer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.WorldRenderer
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType
import net.minecraft.client.renderer.entity.RendererLivingEntity
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.texture.TextureUtil
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.resources.model.IBakedModel
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.ForgeHooksClient
import net.minecraftforge.client.model.pipeline.LightUtil
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
            GlStateManager.translate(-0.4f, 0.5f, 0f)
            if (mc.renderItem.itemModelMesher.getItemModel(recordedPosition.heldItem).isGui3d) {
                GlStateManager.translate(0f, 0.1f, -0.075f)
                GlStateManager.rotate(20f, -1f, 0f, 0f)
                GlStateManager.scale(1.8f, 1.8f, 1.8f)
            }
            renderItem(recordedPosition.heldItem)
            GlStateManager.popMatrix()
        }


        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f)
        GlStateManager.color(1f, 1f, 1f, 1f)
        GlStateManager.depthMask(true)
        GlStateManager.disableBlend()
        GlStateManager.popMatrix()
    }

    private fun renderItem(item: ItemStack) {
        var model = mc.renderItem.itemModelMesher.getItemModel(item)

        mc.textureManager.bindTexture(TextureMap.locationBlocksTexture)
        mc.textureManager.getTexture(TextureMap.locationBlocksTexture).setBlurMipmap(false, false)
        preTransformItem(model)
        GlStateManager.enableRescaleNormal()
        GlStateManager.alphaFunc(516, 0.1f)
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.pushMatrix()
        model = ForgeHooksClient.handleCameraTransforms(model, TransformType.THIRD_PERSON)
        renderItemModel(item, model)
        GlStateManager.cullFace(1029)
        GlStateManager.popMatrix()
        GlStateManager.disableRescaleNormal()
        GlStateManager.disableBlend()
        mc.textureManager.bindTexture(TextureMap.locationBlocksTexture)
        mc.textureManager.getTexture(TextureMap.locationBlocksTexture).restoreLastBlurMipmap()
    }

    private fun preTransformItem(model: IBakedModel) {
        if (!model.isGui3d) {
            GlStateManager.scale(2.0f, 2.0f, 2.0f)
        }

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1f)
    }

    private fun renderItemModel(item: ItemStack, model: IBakedModel) {
        GlStateManager.pushMatrix()
        GlStateManager.scale(0.5f, 0.5f, 0.5f)
        if (model.isBuiltInRenderer) {
            GlStateManager.rotate(180.0f, 0.0f, 1.0f, 0.0f)
            GlStateManager.translate(-0.5f, -0.5f, -0.5f)
            GlStateManager.color(1.0f, 1.0f, 1.0f, 0.6f)
            GlStateManager.enableRescaleNormal()
            TileEntityItemStackRenderer.instance.renderByItem(item)
        } else {
            GlStateManager.translate(-0.5f, -0.5f, -0.5f)
            renderModel(model, -1, item)
            if (item.hasEffect()) {
                applyEnchantGlint(model)
            }
        }

        GlStateManager.popMatrix()
    }

    private fun applyEnchantGlint(model: IBakedModel) {
        GlStateManager.depthMask(false)
        GlStateManager.depthFunc(514)
        GlStateManager.disableLighting()
        GlStateManager.blendFunc(768, 1)
        mc.textureManager.bindTexture(ResourceLocation("textures/misc/enchanted_item_glint.png"))
        GlStateManager.matrixMode(5890)
        GlStateManager.pushMatrix()
        GlStateManager.scale(8.0f, 8.0f, 8.0f)
        val f = (Minecraft.getSystemTime() % 3000L).toFloat() / 3000.0f / 8.0f
        GlStateManager.translate(f, 0.0f, 0.0f)
        GlStateManager.rotate(-50.0f, 0.0f, 0.0f, 1.0f)
        renderModel(model, -8372020, null)
        GlStateManager.popMatrix()
        GlStateManager.pushMatrix()
        GlStateManager.scale(8.0f, 8.0f, 8.0f)
        val f1 = (Minecraft.getSystemTime() % 4873L).toFloat() / 4873.0f / 8.0f
        GlStateManager.translate(-f1, 0.0f, 0.0f)
        GlStateManager.rotate(10.0f, 0.0f, 0.0f, 1.0f)
        renderModel(model, -8372020, null)
        GlStateManager.popMatrix()
        GlStateManager.matrixMode(5888)
        GlStateManager.blendFunc(770, 771)
        GlStateManager.enableLighting()
        GlStateManager.depthFunc(515)
        GlStateManager.depthMask(true)
        mc.textureManager.bindTexture(TextureMap.locationBlocksTexture)
    }

    private fun renderModel(model: IBakedModel, color: Int, item: ItemStack?) {
        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer
        worldRenderer.begin(7, DefaultVertexFormats.ITEM)
        val var6 = EnumFacing.entries.toTypedArray()
        val var7 = var6.size

        for (var8 in 0 until var7) {
            val enumFacing = var6[var8]
            renderQuads(worldRenderer, model.getFaceQuads(enumFacing), color, item)
        }

        renderQuads(worldRenderer, model.generalQuads, color, item)
        tessellator.draw()
    }

    private fun renderQuads(renderer: WorldRenderer, quads: List<BakedQuad>, inputColor: Int, stack: ItemStack?) {
        val flag = inputColor == -1 && stack != null
        var i = 0

        while (i < quads.size) {
            val bakedQuad = quads[i]
            var color = inputColor
            if (flag && bakedQuad.hasTintIndex()) {
                color = stack?.item?.getColorFromItemStack(stack, bakedQuad.tintIndex) ?: 16777215
                if (EntityRenderer.anaglyphEnable) {
                    color = TextureUtil.anaglyphColor(color)
                }

                color = color or -16777216
            }
            if (color != -8372020) {
                color = 2583691263.toInt() //set transparency
            }
            LightUtil.renderQuadColor(renderer, bakedQuad, color)
            ++i
        }
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
