package at.hannibal2.skyhanni.utils.render

import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.render.item.atlas.SkyHanniAnimatedAtlasKey
import at.hannibal2.skyhanni.utils.render.item.atlas.SkyHanniAtlasKey
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.state.GuiItemRenderState
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.item.TrackingItemStackRenderState
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import org.joml.Matrix3x2f

open class SkyHanniGuiItemRenderState(
    open val guiItemRenderState: GuiItemRenderState,
    val x: Float,
    val y: Float,
    val rotationVec: Vec3,
    private val translationVec: Vec3,
    val bounceEnvelopeVec: Vec3,
    val scale: Float = 1f,
    passedStableId: Int? = null,
) : PictureInPictureRenderState {
    val stableId = passedStableId?.takeIf { it >= 0 } ?: nextStableId()
    protected val trackingState: TrackingItemStackRenderState? by lazy { guiItemRenderState.itemStackRenderState() }

    open fun getAtlasKey(guiScale: Int): SkyHanniAtlasKey? = trackingState?.let {
        SkyHanniAtlasKey(it.modelIdentity, scale, guiScale, stableId, rotationVec)
    }

    companion object {
        private var counter = 0
        fun nextStableId() = counter++

        internal fun convertTrackingToGUI(
            trackingState: TrackingItemStackRenderState,
            itemStack: ItemStack,
        ) = GuiItemRenderState(
            itemStack.item.name.toString(),
            Matrix3x2f(DrawContextUtils.drawContext.pose()),
            trackingState,
            0,
            0,
            DrawContextUtils.drawContext.scissorStack.peek()
        )
    }

    constructor(
        trackingState: TrackingItemStackRenderState,
        itemStack: ItemStack,
        x: Float,
        y: Float,
        rotationVec: Vec3? = Vec3.ZERO,
        translationVec: Vec3? = Vec3.ZERO,
        bounceEnvelopeVec: Vec3? = Vec3.ZERO,
        scale: Float = 1f,
        passedStableId: Int? = null,
    ) : this(
        convertTrackingToGUI(trackingState, itemStack),
        x, y,
        rotationVec ?: Vec3.ZERO,
        translationVec ?: Vec3.ZERO,
        bounceEnvelopeVec ?: Vec3.ZERO,
        scale, passedStableId
    )

    private val x0 = x.toInt()
    private val x1 = (x + (scale * 16)).toInt()
    private val y0 = y.toInt()
    private val y1 = (y + (scale * 16)).toInt()

    override fun x0() = x0
    override fun x1() = x1
    override fun y0() = y0
    override fun y1() = y1

    override fun scale() = scale * 16
    override fun pose(): Matrix3x2f {
        val base = guiItemRenderState.pose()
        if (translationVec.y == 0.0 && translationVec.x == 0.0) return base
        return Matrix3x2f(base).translate(translationVec.x.toFloat(), translationVec.y.toFloat())
    }

    override fun scissorArea(): ScreenRectangle? = this.guiItemRenderState.scissorArea()
    override fun bounds(): ScreenRectangle? = this.guiItemRenderState.bounds()

    fun getModelIdentity(): Any? = this.trackingState?.modelIdentity
    fun usesBlockLight(): Boolean = this.trackingState?.usesBlockLight() ?: false
    fun isAnimated(): Boolean = this.trackingState?.isAnimated ?: false
    fun setAnimated() = this.trackingState?.setAnimated()
    fun submit(matrices: PoseStack, submitNodeCollector: SubmitNodeCollector, i: Int, j: Int, k: Int) =
        this.trackingState?.submit(matrices, submitNodeCollector, i, j, k)
}

class SkyHanniGuiAnimatedItemRenderState(
    guiItemRenderState: GuiItemRenderState,
    x: Float,
    y: Float,
    rotationVec: Vec3,
    translationVec: Vec3,
    bounceEnvelopeVec: Vec3,
    scale: Float = 1f,
    passedStableId: Int? = null,
    private val frameNumber: Int = 0,
) : SkyHanniGuiItemRenderState(
    guiItemRenderState, x, y, rotationVec, translationVec, bounceEnvelopeVec, scale, passedStableId
) {
    override fun getAtlasKey(guiScale: Int): SkyHanniAnimatedAtlasKey? = trackingState?.let {
        SkyHanniAnimatedAtlasKey(it.modelIdentity, scale, guiScale, stableId, rotationVec, frameNumber)
    }
}
