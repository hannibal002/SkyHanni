package at.hannibal2.skyhanni.utils.render.item

import net.minecraft.client.renderer.feature.FeatureRenderDispatcher
//? if >= 26.2
import net.minecraft.client.renderer.SubmitNodeStorage

//? if < 26.2
//import net.minecraft.client.renderer.MultiBufferSource.BufferSource

internal class SkyHanniItemRenderContext(
    val atlasStates: List<SkyHanniGuiItemRenderState>,
    //? if >= 26.2
    val submitNodeStorage: SubmitNodeStorage,
    //? if < 26.2
    //val bufferSource: BufferSource,
    val featureRenderDispatcher: FeatureRenderDispatcher,
    val frameNumber: Int,
    val guiScale: Int,
)
