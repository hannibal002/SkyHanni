package at.hannibal2.skyhanni.features.gui.electionviewer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

@SkyHanniModule
object ElectionViewerScreen : GuiScreen() {

    private val scaledResolution get() = ScaledResolution(Minecraft.getMinecraft())
    private val windowWidth get() = scaledResolution.scaledWidth
    private val windowHeight get() = scaledResolution.scaledHeight

    private val guiWidth = (windowWidth / (3 / 4f)).toInt()
    private val guiHeight = (windowHeight / (3 / 4f)).toInt()

    private const val PADDING = 10

    private var display: Renderable? = null

    @SubscribeEvent
    fun onOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isInGui()) return

        val position = Position(windowWidth / 2 - guiWidth / 2 - PADDING, windowHeight / 2 - guiHeight / 2 - PADDING)

        display?.let {
            position.renderRenderable(
                it,
                posLabel = "Election Viewer",
                addToGuiManager = false,
            )
        }
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isInGui()) return

        display = Renderable.drawInsideRoundedRect(
            Renderable.doubleLayered(
                Renderable.placeholder(guiWidth, guiHeight),
                Renderable.horizontalContainer(
                    listOf(
                        Renderable.clickable(
                            Renderable.string("Current Mayor"),
                            bypassChecks = true,
                            onClick = {
                                SkyHanniMod.screenToOpen = CurrentMayorScreen
                            },
                        ),
                        Renderable.clickable(
                            Renderable.string("Current Election"),
                            bypassChecks = true,
                            onClick = {
                                SkyHanniMod.screenToOpen = CurrentElectionScreen
                            },
                        ),
                    ),
                    spacing = 10,
                    verticalAlign = VerticalAlignment.CENTER,
                    horizontalAlign = HorizontalAlignment.CENTER,
                ),
            ),
            Color.BLACK.addAlpha(180),
            padding = PADDING,
        )
    }

    fun isInGui() = Minecraft.getMinecraft().currentScreen is ElectionViewerScreen
}
