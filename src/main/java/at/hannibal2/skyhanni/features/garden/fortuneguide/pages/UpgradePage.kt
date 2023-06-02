package at.hannibal2.skyhanni.features.garden.fortuneguide.pages

import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI
import at.hannibal2.skyhanni.features.garden.fortuneguide.FortuneUpgrades
import net.minecraft.util.MathHelper
import org.lwjgl.input.Mouse

class UpgradePage: FFGuideGUI.FFGuidePage() {
    private var pageScroll = 0
    private var scrollVelocity = 0.0
    private var noInputFrames = 0
    private val maxNoInputFrames = 30
    private val baseY = FFGuideGUI.guiTop + 20

    override fun drawPage(mouseX: Int, mouseY: Int, partialTicks: Float) {
//        for ((index, upgrade) in FortuneUpgrades.generateGenericUpgrades()) {
//
//        }
        return

    }




    // works well need to add max scroll, and stop things from rendering if they go above/below a certain point
    //todo add a scroll bar?, test on trackpad, if it works then no
    override fun handleMouseInput() {
        var scrollDelta = 0

        while (Mouse.next()) {
            if (Mouse.getEventDWheel() != 0) {
                scrollDelta += Mouse.getEventDWheel()
                noInputFrames = 0
            }
        }
        scrollVelocity += scrollDelta / 48.0
        scrollVelocity *= 0.95
        pageScroll += scrollVelocity.toInt() + scrollDelta / 24

        noInputFrames++

        if (noInputFrames >= maxNoInputFrames) {
            scrollVelocity *= 0.5
        }

        if (pageScroll > 0) {
            pageScroll = 0
        }

        // todo
//        pageScroll = MathHelper.clamp_int(pageScroll, -100, 0)


        // if over or under either max for scrolling stop them. Maybe let it scroll 1/2 a length more downwards.
        //Make sure this works if there is only a small amount of recommendations
    }
}