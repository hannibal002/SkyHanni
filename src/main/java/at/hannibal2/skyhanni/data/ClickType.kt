package at.hannibal2.skyhanni.data

enum class ClickType {
    LEFT_CLICK, RIGHT_CLICK;
    fun isLeftClick() = this == LEFT_CLICK
    fun isRightClick() = this == RIGHT_CLICK
}