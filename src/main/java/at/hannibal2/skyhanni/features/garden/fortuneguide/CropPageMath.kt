package at.hannibal2.skyhanni.features.garden.fortuneguide

import at.hannibal2.skyhanni.utils.RenderUtils
import net.minecraft.client.Minecraft
import java.io.IOException


class CropPageMath: FFGuideGUI.FFGuidePage() {
    companion object {
        val texttt = mutableListOf<String>()
    }


    override fun drawPage(mouseX: Int, mouseY: Int, partialTicks: Float) {
        RenderUtils.drawScaledItem(texttt, FFGuideGUI.guiLeft + 10, FFGuideGUI.guiTop + 10, FFGuideGUI.screenHeight)




    }


    @Throws(IOException::class)
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        super.mouseClicked(mouseX, mouseY, mouseButton)
        return false
    }
}