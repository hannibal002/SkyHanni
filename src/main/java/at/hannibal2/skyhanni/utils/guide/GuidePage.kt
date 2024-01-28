package at.hannibal2.skyhanni.utils.guide

abstract class GuidePage {
    abstract fun drawPage(mouseX: Int, mouseY: Int)

    abstract fun onSwitch()
}
