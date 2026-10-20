package at.hannibal2.skyhanni.utils.render.item

import net.minecraft.client.renderer.feature.FeatureRenderDispatcher

//? if >= 26.2 {
import net.minecraft.client.renderer.SubmitNodeStorage
//?} else {
/*import net.minecraft.client.renderer.MultiBufferSource.BufferSource
*///?}

internal class SkyHanniItemRenderContext(
    val atlasStates: List<SkyHanniGuiItemRenderState>,
    //~ if < 26.2 'submitNodeStorage: SubmitNodeStorage' -> 'bufferSource: BufferSource'
    val submitNodeStorage: SubmitNodeStorage,
    val featureRenderDispatcher: FeatureRenderDispatcher,
    val frameNumber: Int,
    val guiScale: Int,
)
