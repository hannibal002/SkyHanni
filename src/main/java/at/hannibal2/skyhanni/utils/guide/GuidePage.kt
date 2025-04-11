package at.hannibal2.skyhanni.utils.guide

import at.hannibal2.skyhanni.utils.compat.DrawContext

abstract class GuidePage {
    abstract fun drawPage(context: DrawContext, mouseX: Int, mouseY: Int)

    abstract fun onEnter()

    abstract fun onLeave()

    fun refresh() {
        onLeave()
        onEnter()
    }

}
