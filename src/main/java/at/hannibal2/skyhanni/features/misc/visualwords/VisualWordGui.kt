package at.hannibal2.skyhanni.features.misc.visualwords

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.MathHelper
import org.lwjgl.input.Mouse
import java.io.IOException

open class VisualWordGui : GuiScreen() {
    private var guiLeft = 0
    private var guiTop = 0
    private var screenHeight = 0
    private val sizeX = 360
    private val sizeY = 180

    private var mouseX = 0
    private var mouseY = 0
    private var lastMouseScroll = 0
    private var noMouseScrollFrames = 0

    private var tooltipToDisplay = mutableListOf<String>()

    private var pageScroll = 0
    private var scrollVelocity = 0.0
    private val maxNoInputFrames = 100

    private val modifiedWords = mutableListOf<VisualWord>()

    companion object {
        fun isInGui() = Minecraft.getMinecraft().currentScreen is VisualWordGui
    }

    override fun drawScreen(unusedX: Int, unusedY: Int, partialTicks: Float) {
        super.drawScreen(unusedX, unusedY, partialTicks)
        drawDefaultBackground()
        screenHeight = height
        guiLeft = (width - sizeX) / 2
        guiTop = (height - sizeY) / 2

        mouseX = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth
        mouseY = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1

        GlStateManager.pushMatrix()
        drawRect(guiLeft, guiTop, guiLeft + sizeX, guiTop + sizeY, 0x50000000)

        val adjustedY = guiTop + 30 + pageScroll
        val inverseScale = 1 / 0.75f
        GlStateManager.scale(0.75f, 0.75f, 1f)

        GuiRenderUtils.drawStringCentered("§7Modify Words. Replace the top with the bottom", (guiLeft + 180) * inverseScale, (guiTop + 10) * inverseScale)
        GuiRenderUtils.drawString("§bPhrase", (guiLeft + 30) * inverseScale, (guiTop + 5) * inverseScale)
        GuiRenderUtils.drawString("§1Status", (guiLeft + 310) * inverseScale, (guiTop + 5) * inverseScale)

        for ((index, phrase) in SkyHanniMod.feature.storage.modifiedWords.withIndex()) {
            if (adjustedY + 30 * index < guiTop + 20) continue
            if (adjustedY + 30 * index > guiTop + 155) continue

            val status = if (phrase.enabled) "§2Enabled" else "§4Disabled"

            GuiRenderUtils.drawString(phrase.phrase, (guiLeft + 15) * inverseScale, (adjustedY - 5 + 30 * index) * inverseScale)
            GuiRenderUtils.drawString(phrase.replacement, (guiLeft + 15) * inverseScale, (adjustedY + 5 + 30 * index) * inverseScale)
            GuiRenderUtils.drawString(status, (guiLeft + 310) * inverseScale, (adjustedY + 30 * index) * inverseScale)
        }

        //todo remove
        if (SkyHanniMod.feature.storage.modifiedWords.size < 1) {
            modifiedWords.clear()
            modifiedWords.add(0, VisualWord("§btesting", "Mouse.getX() * width / Minecraft.getMin§becraft().displayWidth", true))
            modifiedWords.add(0, VisualWord("(adj§kustedY + 25 * index) * inverseScale)", "into", false))
            modifiedWords.add(0, VisualWord("Hel§kp me decide if this rend§lering works", "§bDoes this redndering work", true))
            modifiedWords.add(0, VisualWord("testing", "for ((index, phrase) in modifiedWords.wi§bthIndex())", true))
            modifiedWords.add(0, VisualWord("Skipp§bing NEU Kotlin loading", "in develo§kpment environment.", false))
            modifiedWords.add(0, VisualWord("for ((index, phrase) in modifiedWords.withIndex())", "(adjustedY + 25 * index) * inverseScale)", false))
            modifiedWords.add(0, VisualWord("(ad§ujustedY + 25 * index) * inverseScale)", "GuiRenderUtils.drawTooltip(toolti§bpToDisplay, mouseX, mouseY, height)", false))
            modifiedWords.add(0, VisualWord("for ((index, phrase) in modifi§bedWords.withIndex())", "GuiRe§lnderUtils.drawString(", true))
            modifiedWords.add(0, VisualWord("§btesting", "Mouse.getX() * width / Minecraft.getMin§becraft().displayWidth", true))
            modifiedWords.add(0, VisualWord("(adj§kustedY + 25 * index) * inverseScale)", "into", false))
            modifiedWords.add(0, VisualWord("Hel§kp me decide if this rend§lering works", "§bDoes this redndering work", true))
            modifiedWords.add(0, VisualWord("testing", "for ((index, phrase) in modifiedWords.wi§bthIndex())", true))
            modifiedWords.add(0, VisualWord("Skipp§bing NEU Kotlin loading", "in develo§kpment environment.", false))
            modifiedWords.add(0, VisualWord("for ((index, phrase) in modifiedWords.withIndex())", "(adjustedY + 25 * index) * inverseScale)", false))
            modifiedWords.add(0, VisualWord("(ad§ujustedY + 25 * index) * inverseScale)", "GuiRenderUtils.drawTooltip(toolti§bpToDisplay, mouseX, mouseY, height)", false))
            modifiedWords.add(0, VisualWord("for ((index, phrase) in modifi§bedWords.withIndex())", "GuiRe§lnderUtils.drawString(", false))
            SkyHanniMod.feature.storage.modifiedWords = modifiedWords
            SkyHanniMod.feature.storage.seenThis = true
        }

        GlStateManager.scale(inverseScale, inverseScale, 1f)

        scrollScreen()
        
        GlStateManager.popMatrix()

        if (tooltipToDisplay.isNotEmpty()) {
            GuiRenderUtils.drawTooltip(tooltipToDisplay, mouseX, mouseY, height)
            tooltipToDisplay.clear()
        }
    }

    override fun handleMouseInput() {
        super.handleMouseInput()

        if (Mouse.getEventButtonState()) {
            mouseClickEvent()
        }
        if (!Mouse.getEventButtonState()) {
            if (Mouse.getEventDWheel() != 0) {
                lastMouseScroll = Mouse.getEventDWheel()
                noMouseScrollFrames = 0
            }
        }
    }

    @Throws(IOException::class)
    fun mouseClickEvent() {

    }

    private fun scrollScreen() {
        scrollVelocity += lastMouseScroll / 48.0
        scrollVelocity *= 0.95
        pageScroll += scrollVelocity.toInt() + lastMouseScroll / 24

        noMouseScrollFrames++

        if (noMouseScrollFrames >= maxNoInputFrames) {
            scrollVelocity *= 0.75
        }

        if (pageScroll > 0) {
            pageScroll = 0
        }

        pageScroll = MathHelper.clamp_int(pageScroll, -(SkyHanniMod.feature.storage.modifiedWords.size * 30 - 140), 0)
        lastMouseScroll = 0
    }
}