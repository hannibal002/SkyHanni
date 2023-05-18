package at.hannibal2.skyhanni.features.garden.fortuneguide.pages

import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI
import at.hannibal2.skyhanni.utils.RenderUtils
import java.io.IOException


class CarrotPage: FFGuideGUI.FFGuidePage() {
    override fun drawPage(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val tool = GardenAPI.config?.fortune?.farmingTools?.get(1)?.split("\n")?.toMutableList()
        if (tool != null) {
            if (tool.size > 2) {
                val toolName = tool[0]
                val toolItemID = tool[1]
                tool.removeFirst()
                tool.removeFirst()
                RenderUtils.drawScaledItem(tool, FFGuideGUI.guiLeft + 10, FFGuideGUI.guiTop + 10, FFGuideGUI.screenHeight)
            }
        }
    }

    @Throws(IOException::class)
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        super.mouseClicked(mouseX, mouseY, mouseButton)
        return false
    }

    override fun swapMode() {
        TODO("Not yet implemented")
    }
}