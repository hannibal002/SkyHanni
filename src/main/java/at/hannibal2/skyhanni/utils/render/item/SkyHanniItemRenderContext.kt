package at.hannibal2.skyhanni.utils.render.item

import at.hannibal2.skyhanni.utils.render.SkyHanniGuiItemRenderState
import net.minecraft.client.gui.render.state.GuiRenderState
import net.minecraft.client.renderer.MultiBufferSource.BufferSource
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher

internal data class SkyHanniItemRenderContext(
    val states: List<SkyHanniGuiItemRenderState>,
    val guiRenderState: GuiRenderState,
    val bufferSource: BufferSource,
    val featureRenderDispatcher: FeatureRenderDispatcher,
    val frameNumber: Int,
    val guiScale: Int,
    val fallbackStates: MutableList<SkyHanniGuiItemRenderState>,
    var existing: SkyHanniAtlasPosition? = null,
    var key: SkyHanniAtlasKey? = null,
)
