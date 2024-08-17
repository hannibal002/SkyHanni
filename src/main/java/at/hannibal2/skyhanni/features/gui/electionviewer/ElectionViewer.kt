package at.hannibal2.skyhanni.features.gui.electionviewer

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.inventory.wardrobe.CustomWardrobe.centerString
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.ColorUtils.darker
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
object ElectionViewer : GuiScreen() {

    private val scaledResolution get() = ScaledResolution(Minecraft.getMinecraft())
    private val windowWidth get() = scaledResolution.scaledWidth
    private val windowHeight get() = scaledResolution.scaledHeight

    private val guiWidth = (windowWidth / (3 / 4f)).toInt()
    private val guiHeight = (windowHeight / (3 / 4f)).toInt()

    var display: Renderable? = null

    @SubscribeEvent
    fun onOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isInGui()) return

        val position = Position(windowWidth / 2 - guiWidth / 2, windowHeight / 2 - guiHeight / 2)

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
                        createLabeledButton("Current Election", Color.GRAY, onClick = { }),
                        createLabeledButton("Current Mayor", Color.GRAY, onClick = { ChatUtils.chat("balls") }),
                        createLabeledButton("Next Special Mayors", Color.GRAY, onClick = { }),
                        Renderable.clickable(Renderable.string("im stupid"), bypassChecks = true, onClick = { ChatUtils.chat("balls") }),
                    ),
                    spacing = 10,
                    verticalAlign = VerticalAlignment.CENTER,
                    horizontalAlign = HorizontalAlignment.CENTER,
                ),
            ),
            Color.BLACK.addAlpha(100),
        )
    }

    // TODO: Move to renderutils or smth idk, it currently dupes wi th the wardrobe thing
    private fun createLabeledButton(
        text: String,
        hoveredColor: Color = Color(130, 130, 130, 200),
        unhoveredColor: Color = hoveredColor.darker(0.57),
        onClick: () -> Unit,
    ): Renderable {
        val buttonWidth = 120
        val buttonHeight = 60

        val renderable = Renderable.hoverable(
            Renderable.drawInsideRoundedRectWithOutline(
                Renderable.doubleLayered(
                    Renderable.clickable(
                        Renderable.placeholder(buttonWidth, buttonHeight),
                        onClick,
                        bypassChecks = true, // this is so stupid, why is it needed
                    ),
                    centerString(text),
                    false,
                ),
                hoveredColor,
                padding = 5,
                topOutlineColor = Color.PINK.rgb,
                bottomOutlineColor = Color.BLUE.rgb,
                borderOutlineThickness = 2,
                horizontalAlign = HorizontalAlignment.CENTER,
            ),
            Renderable.drawInsideRoundedRect(
                Renderable.doubleLayered(
                    Renderable.placeholder(buttonWidth, buttonHeight),
                    centerString(text),
                ),
                unhoveredColor.darker(0.57),
                padding = 0,
                horizontalAlign = HorizontalAlignment.CENTER,
            ),
        )

        return renderable
    }

    fun isInGui() = Minecraft.getMinecraft().currentScreen is ElectionViewer
}
