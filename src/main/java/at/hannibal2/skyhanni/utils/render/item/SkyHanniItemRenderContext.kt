package at.hannibal2.skyhanni.utils.render.item

import net.minecraft.client.gui.render.state.GuiRenderState
import net.minecraft.client.renderer.MultiBufferSource.BufferSource
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher

internal open class SkyHanniItemRenderContext(
    open val atlasStates: List<SkyHanniGuiItemRenderState>,
    open val fallbackStates: MutableList<SkyHanniGuiItemRenderState>,
    open val guiRenderState: GuiRenderState,
    open val bufferSource: BufferSource,
    open val featureRenderDispatcher: FeatureRenderDispatcher,
    open val frameNumber: Int,
    open val guiScale: Int,
)
