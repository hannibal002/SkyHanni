package at.hannibal2.skyhanni.test.renderable

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import java.awt.Color

@SkyHanniModule(devOnly = true)
object TestText : RenderableTestSuite.TestRenderable("text") {

    override fun renderable() = Renderable.text(
        TextHelper.createGradientText(
            Color.CYAN,
            LorenzColor.LIGHT_PURPLE.toColor(),
            "Really cool gradient that goes from aqua to pink",
        ),
    )
}
