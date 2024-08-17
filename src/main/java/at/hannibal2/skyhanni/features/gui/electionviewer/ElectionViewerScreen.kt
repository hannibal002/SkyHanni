package at.hannibal2.skyhanni.features.gui.electionviewer

import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
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

    var display: Renderable? = null

    @SubscribeEvent
    open fun onOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
    }

    @SubscribeEvent
    open fun onSecondPassed(event: SecondPassedEvent) {
    }

    abstract fun isInGui(): Boolean
}
