package at.hannibal2.skyhanni.utils.render.item

import net.minecraft.client.renderer.SubmitNodeStorage
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher

internal class SkyHanniItemRenderContext(
    val atlasStates: List<SkyHanniGuiItemRenderState>,
    val submitNodeStorage: SubmitNodeStorage,
    val featureRenderDispatcher: FeatureRenderDispatcher,
    val frameNumber: Int,
    val guiScale: Int,
)
