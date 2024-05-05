package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

open class CustomLinesGui : GuiScreen() {

    private var inTextMode = false
        set(value) {
            field = value
            if (value) {
                textBox.makeActive()
            } else {
                textBox.disable()
            }
        }

    private val textBox = TextInput().apply {
        textBox = "haiii"
    }

    companion object {
        fun isInGui() = Minecraft.getMinecraft().currentScreen is CustomLinesGui
    }

    private fun getDisplay(): Renderable {
        val scaledResolution = ScaledResolution(Minecraft.getMinecraft())
        val width = scaledResolution.scaledWidth - 40
        val height = scaledResolution.scaledHeight - 40

        return Renderable.drawInsideRoundedRect(
            Renderable.verticalContainer(
                listOf(
                    Renderable.placeholder(width, height),
                    Renderable.string("Custom Lines"),
                    Renderable.string("Textmode: $inTextMode"),
                    Renderable.clickable(
                        Renderable.string("Input: ${textBox.editText()}"),
                        {
                            inTextMode = !inTextMode
                        },
                        bypassChecks = true
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

        Position(20, 20).renderRenderables(
            listOf(getDisplay()),
            posLabel = "a"
        )
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isInGui()) return

        if (inTextMode) {
            textBox.handle()
            val text = textBox.finalText()
            if (text.isEmpty()) {
                //activeNode?.name = null
            } else {
                //activeNode?.name = text
            }
        }
    }
}
