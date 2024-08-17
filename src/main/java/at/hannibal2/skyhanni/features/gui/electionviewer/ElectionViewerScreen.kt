package at.hannibal2.skyhanni.features.gui.electionviewer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

abstract class ElectionViewerScreen : GuiScreen() {

    private val scaledResolution get() = ScaledResolution(Minecraft.getMinecraft())
    private val windowWidth get() = scaledResolution.scaledWidth
    private val windowHeight get() = scaledResolution.scaledHeight

    val guiWidth get() = (windowWidth * 0.65f).toInt()
    val guiHeight get() = (windowHeight * 0.65f).toInt()

    val PADDING = 10

    open val posLabel = ""

    var display: Renderable? = null

    @SubscribeEvent
    open fun onOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isInGui()) return

        val buttons = createButtons()

        display?.let {
            renderContent(it)
            renderButtons(buttons)
        }
    }

    @SubscribeEvent
    open fun onSecondPassed(event: SecondPassedEvent) {
    }

    abstract fun isInGui(): Boolean

    private fun createButtons(): Renderable {
        val currentScreen = Minecraft.getMinecraft().currentScreen

        val mayorButton = createButton(
            label = "Mayor",
            isActive = currentScreen is CurrentMayorScreen,
            onClick = { SkyHanniMod.screenToOpen = CurrentMayorScreen },
        )

        val electionButton = createButton(
            label = "Election",
            isActive = currentScreen is CurrentElectionScreen,
            onClick = { SkyHanniMod.screenToOpen = CurrentElectionScreen },
        )

        return Renderable.verticalContainer(
            listOf(mayorButton, electionButton),
            spacing = 10,
            verticalAlign = RenderUtils.VerticalAlignment.CENTER,
            horizontalAlign = RenderUtils.HorizontalAlignment.RIGHT,
        )
    }

    private fun createButton(label: String, isActive: Boolean, onClick: () -> Unit) =
        Renderable.clickable(
            Renderable.drawInsideRoundedRect(
                Renderable.string(label, horizontalAlign = RenderUtils.HorizontalAlignment.CENTER),
                if (isActive) Color.GRAY.addAlpha(200) else Color.DARK_GRAY.addAlpha(200),
                padding = 7,
                horizontalAlign = RenderUtils.HorizontalAlignment.RIGHT,
            ),
            onClick = onClick,
            bypassChecks = true,
        )

    private fun renderContent(renderable: Renderable) {
        val position = Position(
            windowWidth / 2 - guiWidth / 2 - PADDING,
            windowHeight / 2 - guiHeight / 2 - PADDING,
        )
        position.renderRenderable(
            renderable,
            posLabel = posLabel,
            addToGuiManager = false,
        )
    }

    private fun renderButtons(buttons: Renderable) {
        val buttonPosition = Position(
            windowWidth / 2 - guiWidth / 2 - PADDING * 2 - buttons.width,
            windowHeight / 2,
        )
        buttonPosition.renderRenderable(
            buttons,
            posLabel = posLabel,
            addToGuiManager = false,
        )
    }
}
