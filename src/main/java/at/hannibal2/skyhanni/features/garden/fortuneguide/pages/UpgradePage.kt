package at.hannibal2.skyhanni.features.garden.fortuneguide.pages

import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI
import at.hannibal2.skyhanni.features.garden.fortuneguide.FortuneUpgrades
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.NumberUtil
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
        val inverseScale = 1 / 0.5f

        GlStateManager.scale(0.5f, 0.5f, 0.5f)
        GuiRenderUtils.drawString("Upgrade", (FFGuideGUI.guiLeft + 80)  * inverseScale, (FFGuideGUI.guiTop + 5)  * inverseScale)
        GuiRenderUtils.drawString("FF increase", (FFGuideGUI.guiLeft + 190)  * inverseScale, (FFGuideGUI.guiTop + 5)  * inverseScale)
        GuiRenderUtils.drawString("Cost per FF", (FFGuideGUI.guiLeft + 225)  * inverseScale, (FFGuideGUI.guiTop + 5)  * inverseScale)
        GuiRenderUtils.drawString("Total cost", (FFGuideGUI.guiLeft + 260)  * inverseScale, (FFGuideGUI.guiTop + 5)  * inverseScale)
        GuiRenderUtils.drawString("Time to earn back", (FFGuideGUI.guiLeft + 295)  * inverseScale, (FFGuideGUI.guiTop + 5)  * inverseScale)

        for ((index, upgrade) in FortuneUpgrades.genericUpgrades.withIndex()) {
            if (adjustedY + 15 * index < FFGuideGUI.guiTop + 20) continue
            if (adjustedY + 15 * index > FFGuideGUI.guiTop + 170) continue
            GuiRenderUtils.drawString(upgrade.description, (FFGuideGUI.guiLeft + 15)  * inverseScale, (adjustedY + 15 * index)  * inverseScale)
            GuiRenderUtils.drawString(DecimalFormat("0.##").format(upgrade.fortuneIncrease), (FFGuideGUI.guiLeft + 200)  * inverseScale, (adjustedY + 15 * index)  * inverseScale)
            GuiRenderUtils.drawString(upgrade.costPerFF?.let { NumberUtil.format(it) } ?: "unknown", (FFGuideGUI.guiLeft + 235)  * inverseScale, (adjustedY + 15 * index)  * inverseScale)
            GuiRenderUtils.drawString(upgrade.cost?.let { NumberUtil.format(it) } ?: "unknown", (FFGuideGUI.guiLeft + 270)  * inverseScale, (adjustedY + 15 * index)  * inverseScale)
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