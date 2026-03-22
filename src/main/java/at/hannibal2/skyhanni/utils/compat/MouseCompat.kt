package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.events.minecraft.KeyDownEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.utils.DelayedRun
import net.minecraft.client.Minecraft
import net.minecraft.client.input.MouseButtonInfo

object MouseCompat {
    private const val NUMBER_OF_MOUSE_BUTTONS = 6

    private val buttonStates = BooleanArray(NUMBER_OF_MOUSE_BUTTONS)

    @JvmStatic
    var deltaMouseY = 0.0
    @JvmStatic
    var deltaMouseX = 0.0
    @JvmStatic
    var scroll = 0.0
    @JvmStatic
    var timeDelta = 0.0

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
        DelayedRun.runNextTickEnd { scroll = 0.0 }
        return delta.toInt() * 120
    }

    fun getX(): Int {
        return mouse.xpos().toInt()
    }

    fun getY(): Int {
        return mouse.ypos().toInt()
    }

    fun getEventButtonState(): Boolean = buttonStates.any { it }
    fun getEventNanoseconds(): Long = timeDelta.toLong()

    fun getEventDY(): Int {
        return deltaMouseY.toInt()
    }

    @JvmStatic
    fun handleMouseButton(input: MouseButtonInfo, action: Int) {
        val button: Int = input.button()
        if (action == 1) {
            setButtonState(button, true)
            KeyDownEvent(button).post()
            KeyPressEvent(button).post()
        } else {
            KeyPressEvent(button).post()
            DelayedRun.runNextTickEnd {
                setButtonState(button, false)
            }
        }
    }
}
