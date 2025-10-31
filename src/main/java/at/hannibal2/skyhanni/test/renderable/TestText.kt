package at.hannibal2.hanni.test.renderable

import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.chat.TextHelper
import at.hannibal2.hanni.utils.renderables.Renderable
import at.hannibal2.hanni.utils.renderables.primitives.text
import java.awt.Color

@HanniModule(devOnly = true)
object TestText : RenderableTestSuite.TestRenderable("text") {

    override fun renderable() = Renderable.text(
        TextHelper.createGradientText(
            Color.CYAN,
            LorenzColor.LIGHT_PURPLE.toColor(),
            "Really cool gradient that goes from aqua to pink",
        ),
    )
}
