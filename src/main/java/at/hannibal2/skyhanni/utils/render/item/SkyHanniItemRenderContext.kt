package at.hannibal2.skyhanni.utils.render.item

import net.minecraft.client.renderer.MultiBufferSource.BufferSource
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher

internal class SkyHanniItemRenderContext(
    val atlasStates: List<SkyHanniGuiItemRenderState>,
    val bufferSource: BufferSource,
    val featureRenderDispatcher: FeatureRenderDispatcher,
    val frameNumber: Int,
    val guiScale: Int,
)
