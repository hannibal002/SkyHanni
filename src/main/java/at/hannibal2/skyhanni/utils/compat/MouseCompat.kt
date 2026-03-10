package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.events.minecraft.KeyDownEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.utils.DelayedRun
import net.minecraft.client.Minecraft
import net.minecraft.client.input.MouseButtonInfo

object MouseCompat {
    private const val NUMBER_OF_MOUSE_BUTTONS = 6

    var deltaMouseY = 0.0
    var deltaMouseX = 0.0
    var scroll = 0.0
    var timeDelta = 0.0
    private val buttonStates = BooleanArray(NUMBER_OF_MOUSE_BUTTONS)

    private val mouse by lazy {
        Minecraft.getInstance().mouseHandler
    }

    fun isButtonDown(button: Int): Boolean {
        if (button in 0..5) return buttonStates[button]
        return false
    }

    fun setButtonState(button: Int, down: Boolean) {
        if (button in 0..5) {
            buttonStates[button] = down
        }
    }

    fun getScrollDelta(): Int {
        val delta = scroll
        DelayedRun.runNextTickOld { scroll = 0.0 }
        return delta.toInt() * 120
    }

    fun getX(): Int {
        return mouse.xpos().toInt()
    }

    fun getY(): Int {
        return mouse.ypos().toInt()
    }

    // I have no clue what the difference between getx and geteventx is on 1.8.9
    // on 1.8.9 they are pretty much the same (they are the exact same when the mouse is still)
    fun getEventX(): Int = getX()
    fun getEventY(): Int = getY()

    fun getEventButtonState(): Boolean = buttonStates.any { it }
    fun getEventNanoseconds(): Long = timeDelta.toLong()

    fun getEventDY(): Int {
        return deltaMouseY.toInt()
    }

    fun handleMouseButton(input: MouseButtonInfo, action: Int) {
        val button: Int = input.button()
        if (action == 1) {
            setButtonState(button, true)
            KeyDownEvent(button).post()
            KeyPressEvent(button).post()
        } else {
            KeyPressEvent(button).post()
            DelayedRun.runNextTickOld {
                setButtonState(button, false)
            }
        }
    }
}
