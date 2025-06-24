package at.hannibal2.skyhanni.data.title

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.GuiEditManager
import at.hannibal2.skyhanni.utils.ColorUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.farPast
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.now
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableString
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class TitleIntention(
    val displayName: String,
    val internalName: String,
)

// todo 1.21 impl needed
open class TitleContext(
    private var titleText: String = "",
    private var subtitleText: String? = null,
    private var intention: TitleIntention? = null,
    var duration: Duration = 1.seconds,
    val weight: Double = 1.0,
    var discardOnWorldChange: Boolean = true,
) {
    var endTime: SimpleTimeMark? = null
    private var hasBeenReQueued: Boolean = false

    open val alive get() = endTime != null && (endTime?.isInPast() == false)

    open fun getTitleText(): String = titleText
    open fun getSubtitleText(): String? = subtitleText
    open fun start() {
        if (endTime == null || endTime?.isInPast() == true) {
            endTime = now() + duration
        }
    }
    open fun stop() {
        endTime = farPast()
    }
    open fun processRequeue(): Boolean {
        if (hasBeenReQueued) return false
        hasBeenReQueued = true
        duration = endTime?.timeUntil() ?: Duration.ZERO
        return true
    }

    override fun equals(other: Any?): Boolean = this === other || other is TitleContext && this.dataEquivalent(other)
    override fun hashCode(): Int =
        titleText.hashCode() * 31 + (subtitleText?.hashCode() ?: 0) * 31 +
            duration.hashCode() * 31 + weight.hashCode()

    protected fun dataEquivalent(other: TitleContext): Boolean = titleText == other.titleText &&
        subtitleText == other.subtitleText &&
        duration == other.duration &&
        weight == other.weight

    private fun TitleIntention.getPositionOrNull(location: TitleManager.TitleLocation): Position? {
        val intentionPosition = TitleManager.guiConfig.titleIntentionPositions[location]
        return intentionPosition?.get(this.internalName)
    }

    fun tryRenderGlobalTitle() {
        val intentionPosition = intention?.getPositionOrNull(TitleManager.TitleLocation.GLOBAL)
        val position = intentionPosition ?: TitleManager.guiConfig.titlePosition
        val guiWidth = GuiScreenUtils.scaledWindowWidth

        val mainScalar = position.scale * 3.0
        val subScalar = mainScalar * 0.75f

        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0)
        DrawContextUtils.pushPop {
            val mainTextRenderable = RenderableString(
                getTitleText(),
                scale = mainScalar,
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
            )

            val subtitleRenderable: Renderable? = getSubtitleText()?.let {
                RenderableString(
                    it,
                    scale = subScalar,
                    horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                )
            }

            val targetRenderable = if (subtitleRenderable == null) mainTextRenderable
            else VerticalContainerRenderable(
                listOf(mainTextRenderable, subtitleRenderable),
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                verticalAlign = RenderUtils.VerticalAlignment.CENTER,
            )

            val renderableWidth = targetRenderable.width
            val renderableHeight = targetRenderable.height

            val posX = (guiWidth - renderableWidth) / 2
            var posY = position.y
            // moving the display to the bottom half of your screen is futile
            if (posY < 0) {
                posY = 100
            }
            if (posX != position.x || posY != position.y) {
                position.set(Position(posX, posY, scale = position.scale))
            }

            DrawContextUtils.translate(posX.toFloat(), posY.toFloat(), 0f)
            targetRenderable.renderXYAligned(0, 0, renderableWidth, renderableHeight)

            if (intentionPosition != null) {
                // Intention is never null here, but it's mutable so
                val qualifiedIntention = intention ?: return
                val intentionFormat = "Title: ${qualifiedIntention.displayName}"
                GuiEditManager.add(intentionPosition, intentionFormat, targetRenderable.width, targetRenderable.height)
            } else {
                GuiEditManager.add(position, "Title", targetRenderable.width, targetRenderable.height)
            }
        }
    }

    fun tryRenderInventoryTitle() {
        val gui = Minecraft.getMinecraft().currentScreen as? GuiContainer ?: return

        val stringRenderable = VerticalContainerRenderable(
            listOfNotNull(
                RenderableString(
                    getTitleText(),
                    1.5,
                    horizontalAlign = RenderUtils.HorizontalAlignment.CENTER
                ),
                getSubtitleText()?.let {
                    RenderableString(it, horizontalAlign = RenderUtils.HorizontalAlignment.CENTER)
                }
            ),
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
        )

        val translation = stringRenderable.height.toFloat() + 125f

        DrawContextUtils.pushPop {
            DrawContextUtils.translate(0f, -translation, 500f)
            Renderable.drawInsideRoundedRect(
                stringRenderable,
                ColorUtils.TRANSPARENT_COLOR,
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                verticalAlign = RenderUtils.VerticalAlignment.CENTER,
            ).renderXYAligned(0, 125, gui.width, gui.height)

            DrawContextUtils.translate(0f, translation, -500f)
        }
    }

    override fun toString(): String = buildString {
        append("Title: ${getTitleText()}\n")
        append("Subtitle: ${getSubtitleText()}\n")
        append("Duration: ${duration.inWholeSeconds}s\n")
        append("Weight: ${weight}\n")
        append("End Time: ${endTime?.timeUntil()?.inWholeSeconds ?: 0.0}s\n")
    }
}
