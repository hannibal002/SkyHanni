package at.hannibal2.skyhanni.test.renderable

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule(devOnly = true)
object TestRenderAnimatedItem : RenderableTestSuite.TestRenderable("animated_item") {

    override fun renderable() = TestRenderItems.animatedItemStackRenderable

}
