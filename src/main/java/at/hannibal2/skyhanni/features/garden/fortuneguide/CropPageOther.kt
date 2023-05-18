package at.hannibal2.skyhanni.features.garden.fortuneguide

import java.io.IOException


class CropPageOther: FFGuideGUI.FFGuidePage() {
    override fun drawPage(mouseX: Int, mouseY: Int, partialTicks: Float) {
        println(FFGuideGUI.selectedPage)
        return
    }


    @Throws(IOException::class)
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        super.mouseClicked(mouseX, mouseY, mouseButton)
        return false
    }
}