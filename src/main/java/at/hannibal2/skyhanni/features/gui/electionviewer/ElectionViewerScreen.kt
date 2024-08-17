package at.hannibal2.skyhanni.features.gui.electionviewer

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

abstract class ElectionViewerScreen() : GuiScreen() {

    val scaledResolution get() = ScaledResolution(Minecraft.getMinecraft())
    val windowWidth get() = scaledResolution.scaledWidth
    val windowHeight get() = scaledResolution.scaledHeight

    val guiWidth = (windowWidth / (3 / 4f)).toInt()
    val guiHeight = (windowHeight / (3 / 4f)).toInt()

    val PADDING = 10

    open val posLabel = ""

    var display: Renderable? = null

    @SubscribeEvent
    open fun onOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isInGui()) return

        display?.let {
            val position = Position(windowWidth / 2 - guiWidth / 2 - PADDING, windowHeight / 2 - guiHeight / 2 - PADDING)

            position.renderRenderable(
                it,
                posLabel = posLabel,
                addToGuiManager = false,
            )
        }
    }

    @SubscribeEvent
    open fun onSecondPassed(event: SecondPassedEvent) {
    }

    abstract fun isInGui(): Boolean
}
