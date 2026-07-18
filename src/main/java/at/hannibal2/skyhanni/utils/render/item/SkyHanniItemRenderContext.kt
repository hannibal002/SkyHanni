package at.hannibal2.skyhanni.utils.render.item

import net.minecraft.client.renderer.feature.FeatureRenderDispatcher
import net.minecraft.client.renderer.MultiBufferSource.BufferSource

internal class SkyHanniItemRenderContext(
    val atlasStates: List<SkyHanniGuiItemRenderState>,
    val bufferSource: BufferSource,
    val featureRenderDispatcher: FeatureRenderDispatcher,
    val frameNumber: Int,
    val guiScale: Int,
)
