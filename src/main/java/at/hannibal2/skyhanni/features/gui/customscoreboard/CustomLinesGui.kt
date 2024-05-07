package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LorenzColor
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
    private val columnSpacing = 10 // temp value
    private val maxSecondColumnWidth = 300 // temp value

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
        val secondColumn = createRenderableSecondColumn()
        val secondColumnWidth = secondColumn.width
        val firstColumnMaxWidth = guiWidth - secondColumnWidth - columnSpacing

        val secondColumnRenderable = Renderable.horizontalContainer(
            listOf(
                Renderable.placeholder(guiWidth - secondColumnWidth, 0),
                secondColumn
            )
        )

        val firstRenderable = createRenderableFirstColumn(firstColumnMaxWidth)

        val fullRenderable = Renderable.drawInsideRoundedRect(
            Renderable.doubleLayered(
                Renderable.placeholder(guiWidth, guiHeight),
                Renderable.doubleLayered(
                    secondColumnRenderable,
                    firstRenderable
                ),
            ),
            LorenzColor.BLACK.addOpacity(100),
            padding = 10,
        )

        return fullRenderable
    }

    private fun createRenderableSecondColumn(): Renderable {
        val list = mutableListOf<Renderable>()
        CustomLines.replacements.forEach {
            list.add(
                Renderable.clickable(
                    Renderable.wrappedString(
                        "${it.third} | ${it.second.invoke()}", maxSecondColumnWidth
                    ),
                    {
                        if (inTextMode) {
                            textBox.textBox += it.first
                        }
                    },
                    bypassChecks = true
                )
            )
        }
        val replacementsRenderable = Renderable.verticalContainer(list)

        return Renderable.verticalContainer(
            listOf(
                Renderable.string("Replacements"),
                replacementsRenderable
            )
        )
    }

    private fun createRenderableFirstColumn(maxWidth: Int): Renderable {
        val input = Renderable.wrappedString("Input: ${textBox.editText()}", width = maxWidth)
        return Renderable.verticalContainer(
            listOf(
                Renderable.string("Custom Lines"),
                Renderable.clickable(
                    if (inTextMode) {
                        Renderable.underlined(input)
                    } else {
                        input
                    },
                    {
                        inTextMode = !inTextMode
                    },
                    bypassChecks = true
                )
            )
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
            CustomScoreboard.customlineConfig.customLine1 = textBox.finalText().replace("§", "&")
        }
    }
}
