package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.render.atlas.SkyHanniRoundedShapeAtlas
import at.hannibal2.skyhanni.utils.render.atlas.SkyHanniRoundedShapeAtlasKey
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.renderer.state.gui.GuiRenderState
import org.joml.Matrix3x2f

/**
 * Coordinates rounded shape rendering across frames.
 *
 * Shapes are either served from the [SkyHanniRoundedShapeAtlas] (pre-rendered once, then
 * blitted cheaply each frame) or submitted as direct deferred [net.minecraft.client.renderer.state.gui.GuiElementRenderState]s.
 *
 * ## Behavior
 * [preRenderAtlas] is hooked into the preparePictureInPicture phase alongside the item atlas.
 * On the first frame a shape is seen it is registered as pending; on the next frame it is
 * pre-rendered into the atlas. Subsequent frames submit a cheap GPU blit instead of a full
 * shader draw. Shapes that are not eligible for atlasing (animated arcs, gradient rects) fall
 * back to deferred [ShaderRenderUtils] submission on every frame.
 *
 * ## Thread safety
 * Must be called from the render thread only.
 */
@SkyHanniModule
object SkyHanniRoundedShapeRenderManager {

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Rounded Shape Atlas")
        event.addIrrelevant {
            atlas.atlasDebugInfo().onEach(::add)
        }
    }

    private val atlas by lazy { SkyHanniRoundedShapeAtlas() }

    // Shapes collected this frame that could be atlased next frame
    private val pendingKeys = ArrayList<SkyHanniRoundedShapeAtlasKey>()

    /**
     * Pre-renders all pending static shapes to the atlas.
     * Call once per frame, before the GUI render pass.
     */
    fun preRenderAtlas() {
        atlas.preRenderShapes(pendingKeys)
        pendingKeys.clear()
    }

    /**
     * Attempts to submit a blit for [key] from the atlas.
     * Returns false if the shape is not yet in the atlas; the caller should then
     * submit a deferred [net.minecraft.client.renderer.state.gui.GuiElementRenderState] via [ShaderRenderUtils] instead.
     *
     * Registers [key] as a candidate for atlas pre-rendering next frame.
     *
     * @param key the atlas key identifying the shape to blit
     * @param guiRenderState the current frame's GUI render state queue
     * @param pose the current GUI pose matrix for coordinate transformation
     * @param x0 left screen coordinate in logical GUI pixels
     * @param y0 top screen coordinate in logical GUI pixels
     * @param x1 right screen coordinate in logical GUI pixels
     * @param y1 bottom screen coordinate in logical GUI pixels
     * @param alpha ARGB-packed alpha multiplier; use -1 for fully opaque white
     * @param scissor optional scissor rectangle to clip the blit
     */
    fun submitBlit(
        key: SkyHanniRoundedShapeAtlasKey,
        guiRenderState: GuiRenderState,
        pose: Matrix3x2f,
        x0: Int, y0: Int, x1: Int, y1: Int,
        alpha: Int,
        scissor: ScreenRectangle?,
    ): Boolean {
        pendingKeys.add(key)
        return atlas.submitBlit(key, guiRenderState, pose, x0, y0, x1, y1, alpha, scissor)
    }

    fun invalidateAtlas() {
        atlas.invalidate()
        pendingKeys.clear()
    }

    fun closeAtlas() {
        atlas.close()
        pendingKeys.clear()
    }
}
