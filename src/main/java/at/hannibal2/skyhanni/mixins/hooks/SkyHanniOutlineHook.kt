package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.test.command.ErrorManager
import com.llamalad7.mixinextras.injector.wrapoperation.Operation
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.rendertype.RenderType

//? if >= 26.2 {
import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.equalsOneOf
import com.mojang.blaze3d.GpuFormat
import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.CompareOp
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.client.renderer.BindGroupLayouts
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.StagedVertexBuffer
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer
import net.minecraft.client.renderer.rendertype.PreparedRenderType
import net.minecraft.resources.Identifier
//?} else {
/*import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.GpuTextureView
import com.mojang.blaze3d.textures.TextureFormat
import net.minecraft.client.renderer.OutlineBufferSource
*///?}

// The idea and implementation for this class was inspired by Skyblocker.
// This implementation has been modified from the original Skyblocker code to work across multiple versions.
object SkyHanniOutlineHook {

    //? if < 26.2 {
    /*class SkyHanniOutlineVertexConsumerProvider : OutlineBufferSource() {
        override fun endOutlineBatch() {
            beginRendering()
            super.endOutlineBatch()
            finishRendering()
        }

        override fun getBuffer(renderLayer: RenderType): VertexConsumer {
            beginRendering()
            val returnVal = super.getBuffer(renderLayer)
            finishRendering()
            return returnVal
        }
    }

    @JvmStatic
    val vertexConsumers by lazy {
        SkyHanniOutlineVertexConsumerProvider()
    }
    *///?}

    private var customDepthAttachment: GpuTexture? = null

    private var customDepthAttachmentView: GpuTextureView? = null

    //~ if < 26.2 'GpuFormat' -> 'TextureFormat'
    private var customDepthAttachmentFormat: GpuFormat? = null

    @JvmStatic
    var isCurrentlyActive = false
        private set

    private fun beginRendering() {
        val depthAttachmentView = customDepthAttachmentView ?: return
        isCurrentlyActive = true
        RenderSystem.outputDepthTextureOverride = depthAttachmentView
    }

    private fun finishRendering() {
        isCurrentlyActive = false
        RenderSystem.outputDepthTextureOverride = null
    }

    //? if >= 26.2 {
    @JvmStatic
    fun wrapRendering(
        // Required for Java interop with Operation<Void>
        @Suppress("ForbiddenVoid")
        original: Operation<Void>,
        renderType: PreparedRenderType,
        executeInfo: StagedVertexBuffer.ExecuteInfo,
    ) {
        val hasCustomOutline = renderType.pipeline().isCustomOutlinePipeline

        if (hasCustomOutline) beginRendering()
        try {
            original.call(renderType, executeInfo)
        } finally {
            if (hasCustomOutline) finishRendering()
        }
    }
    //?}

    private var lastWidth = 0
    private var lastHeight = 0

    @JvmStatic
    fun checkIfDepthAttachmentNeedsUpdating() {
        //~ if < 26.2 'gameRenderer.mainRenderTarget()' -> 'mainRenderTarget'
        val gpuTexture = Minecraft.getInstance().gameRenderer.mainRenderTarget().depthTexture ?: return
        val width = gpuTexture.getWidth(0)
        val height = gpuTexture.getHeight(0)
        val format = gpuTexture.format
        try {
            if (
                customDepthAttachment == null ||
                width != lastWidth ||
                height != lastHeight ||
                format != customDepthAttachmentFormat
            ) {
                lastWidth = width
                lastHeight = height
                customDepthAttachmentFormat = format
                updateDepthAttachment(format)
            }
            val depthAttachment = customDepthAttachment ?: return
            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
                gpuTexture,
                depthAttachment,
                0, 0, 0, 0, 0, lastWidth, lastHeight,
            )
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Failed to copy depth attachment")
        }
    }

    //~ if < 26.2 'GpuFormat' -> 'TextureFormat'
    private fun updateDepthAttachment(format: GpuFormat) {
        try {
            customDepthAttachment?.let {
                it.close()
                customDepthAttachmentView?.close()
            }
            val device = RenderSystem.getDevice()
            val depthAttachment = device.createTexture(
                "SkyHanni Custom Depth",
                GpuTexture.USAGE_RENDER_ATTACHMENT or GpuTexture.USAGE_COPY_DST or GpuTexture.USAGE_TEXTURE_BINDING,
                format,
                lastWidth, lastHeight, 1, 1,
            )
            customDepthAttachment = depthAttachment
            customDepthAttachmentView = device.createTextureView(depthAttachment)
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Failed to update outline depth attachment")
        }
    }

    //? if >= 26.2 {
    private val customOutlineCullPipeline: RenderPipeline = createCustomOutlinePipeline("custom_outline_cull", true)

    private val customOutlineNoCullPipeline: RenderPipeline = createCustomOutlinePipeline("custom_outline_no_cull", false)

    @JvmStatic
    fun getCustomOutlinePipeline(pipeline: RenderPipeline): RenderPipeline {
        if (!isCurrentlyActive) return pipeline
        return when (pipeline) {
            RenderPipelines.OUTLINE_CULL -> customOutlineCullPipeline
            RenderPipelines.OUTLINE_NO_CULL -> customOutlineNoCullPipeline
            else -> pipeline
        }
    }

    private var customOutlineBuildDepth = 0

    @JvmStatic
    fun wrapCustomOutlineBuild(
        original: Operation<VertexConsumer>,
        //~ if < 26.2 'RenderTypeFeatureRenderer<*>' -> 'OutlineBufferSource'
        instance: RenderTypeFeatureRenderer<*>,
        renderType: RenderType,
    ): VertexConsumer {
        customOutlineBuildDepth++
        try {
            return original.call(instance, renderType)
        } finally {
            customOutlineBuildDepth--
        }
    }

    @JvmStatic
    fun getCustomOutlinePipelineForBuild(pipeline: RenderPipeline): RenderPipeline {
        if (customOutlineBuildDepth <= 0) return pipeline
        return when (pipeline) {
            RenderPipelines.OUTLINE_CULL -> customOutlineCullPipeline
            RenderPipelines.OUTLINE_NO_CULL -> customOutlineNoCullPipeline
            else -> pipeline
        }
    }

    //? if >= 26.2 {
    private val RenderPipeline.isCustomOutlinePipeline: Boolean get() =
        equalsOneOf(customOutlineCullPipeline, customOutlineNoCullPipeline)
    //?}

    @JvmStatic
    fun ensureCustomOutlinePipelinesRegistered() {
        customOutlineCullPipeline
        customOutlineNoCullPipeline
    }

    private fun createCustomOutlinePipeline(path: String, cull: Boolean): RenderPipeline =
        RenderPipelines.register(
            RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath(SkyHanniMod.MODID, "pipeline/$path"))
                .withBindGroupLayout(BindGroupLayouts.GLOBALS)
                .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                .withVertexShader("core/rendertype_outline")
                .withFragmentShader("core/rendertype_outline")
                .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                //~ if < 26.2 'GREATER_THAN_OR_EQUAL' -> 'LESS_THAN_OR_EQUAL'
                .withDepthStencilState(DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
                .withCull(cull)
                .build(),
        )
    //?}
}
