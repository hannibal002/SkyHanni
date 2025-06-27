package at.hannibal2.skyhanni.utils.compat

import org.lwjgl.input.Mouse

object MouseCompat {
    fun isButtonDown(button: Int): Boolean = Mouse.isButtonDown(button)

    fun getX(): Int = Mouse.getX()
    fun getY(): Int = Mouse.getY()

    fun getEventButtonState(): Boolean = Mouse.getEventButtonState()
    fun getEventNanoseconds(): Long = Mouse.getEventNanoseconds()

    fun getEventX(): Int = Mouse.getEventX()
    fun getEventY(): Int = Mouse.getEventY()

    fun getEventDY(): Int = Mouse.getEventDY()
    fun getScrollDelta(): Int = Mouse.getEventDWheel()

    fun getEventButton(): Int = Mouse.getEventButton()
}
