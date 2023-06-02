package at.hannibal2.skyhanni.features.garden.fortuneguide.pages

import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI
import at.hannibal2.skyhanni.features.garden.fortuneguide.FortuneUpgrades
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.MathHelper
import java.text.DecimalFormat

class UpgradePage: FFGuideGUI.FFGuidePage() {
    private var pageScroll = 0
    private var scrollVelocity = 0.0
    private val maxNoInputFrames = 100

    override fun drawPage(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val adjustedY = FFGuideGUI.guiTop + 20 + pageScroll
        val inverseScale = 1 / 0.7f

        GlStateManager.scale(0.7f, 0.7f, 0.7f)
        for ((index, upgrade) in FortuneUpgrades.genericUpgrades.withIndex()) {
            // should never be null
            GuiRenderUtils.drawString(upgrade.description, (FFGuideGUI.guiLeft + 15)  * inverseScale, (adjustedY + 15 * index)  * inverseScale)
            GuiRenderUtils.drawString(DecimalFormat("0.##").format(upgrade.fortuneIncrease), (FFGuideGUI.guiLeft + 220)  * inverseScale, (adjustedY + 15 * index)  * inverseScale)
            GuiRenderUtils.drawString(upgrade.cost?.addSeparators() ?: "unknown", (FFGuideGUI.guiLeft + 250)  * inverseScale, (adjustedY + 15 * index)  * inverseScale)
        }
        GlStateManager.scale(inverseScale, inverseScale, inverseScale)
        scrollScreen()
    }

    private fun scrollScreen() {
        scrollVelocity += FFGuideGUI.lastMouseScroll / 48.0
        scrollVelocity *= 0.95
        pageScroll += scrollVelocity.toInt() + FFGuideGUI.lastMouseScroll / 24

        FFGuideGUI.noMouseScrollFrames++

        if (FFGuideGUI.noMouseScrollFrames >= maxNoInputFrames) {
            scrollVelocity *= 0.75
        }

        if (pageScroll > 0) {
            pageScroll = 0
        }

        // todo
        // pageScroll = MathHelper.clamp_int(pageScroll, -100, 0)
        FFGuideGUI.lastMouseScroll = 0
    }

    // works well need to add max scroll, and stop things from rendering if they go above/below a certain point
    //todo add a scroll bar?, test on trackpad, if it works then no
}