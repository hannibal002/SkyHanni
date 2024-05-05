package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

open class CustomLinesGui : GuiScreen() {

    private val scaledResolution get() = ScaledResolution(Minecraft.getMinecraft())
    private val windowWidth get() = scaledResolution.scaledWidth
    private val windowHeight get() = scaledResolution.scaledHeight

    private val guiWidth = (windowWidth / (3 / 4f)).toInt()
    private val guiHeight = (windowHeight / (3 / 4f)).toInt()

    private var inTextMode = false
        set(value) {
            field = value
            if (value) {
                textBox.textBox = CustomScoreboard.customlineConfig.customLine1
                textBox.makeActive()
            } else {
                textBox.disable()
            }
        }

    private val textBox = TextInput()

    companion object {
        fun isInGui() = Minecraft.getMinecraft().currentScreen is CustomLinesGui
    }

    private fun getDisplay(): Renderable {

        val text = Renderable.string("Input: ${textBox.editText()}")
        return Renderable.drawInsideRoundedRect(
            Renderable.doubleLayered(
                Renderable.placeholder(guiWidth, guiHeight),
                Renderable.verticalContainer(
                    listOf(
                        Renderable.string("Custom Lines"),
                        Renderable.clickable(
                            if (inTextMode) {
                                Renderable.underlined(text)
                            } else {
                                text
                            },
                            {
                                inTextMode = !inTextMode
                            },
                            bypassChecks = true
                        )
                    )
                )
            ),
            LorenzColor.BLACK.addOpacity(100),
            padding = 10,
            verticalAlign = RenderUtils.VerticalAlignment.CENTER,
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
        )
    }

    @SubscribeEvent
    fun onOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isInGui()) return

        val position = Position(windowWidth / 2 - guiWidth / 2, windowHeight / 2 - guiHeight / 2)

        position.renderRenderable(
            getDisplay(),
            posLabel = "CustomLinesGui",
            addToGuiManager = false
        )
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isInGui()) {
            inTextMode = false
            return
        }

        if (inTextMode) {
            textBox.handle()
            CustomScoreboard.customlineConfig.customLine1 = textBox.finalText()
        }
    }
}
