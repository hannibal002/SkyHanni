package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.events.minecraft.KeyDownEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.utils.DelayedRun
import net.minecraft.client.Minecraft
import net.minecraft.client.input.MouseButtonInfo
import kotlin.math.sign

object MouseCompat {
    private const val NUMBER_OF_MOUSE_BUTTONS = 6

    var deltaMouseY = 0.0
        set(value) {
            field = value
            mouseMoveEventId++
        }
    var deltaMouseX = 0.0
    var scroll = 0.0
        set(value) {
            field = value
            if (value != 0.0) scrollEventId++
        }
    private var mouseMoveEventId = 0L
    private var scrollEventId = 0L
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
        return (getPreciseScrollDelta() * 120).toInt()
    }

    fun getPreciseScrollDelta(): Double {
        val delta = scroll
        DelayedRun.runNextTickOld { scroll = 0.0 }
        val options = Minecraft.getInstance().options
        val scrollAmount = if (options.discreteMouseScroll().get()) delta.sign else delta
        return scrollAmount * options.mouseWheelSensitivity().get()
    }

    fun hasScrollDelta(): Boolean = scroll != 0.0

    fun getMouseMoveEventId(): Long = mouseMoveEventId

    fun getScrollEventId(): Long = scrollEventId

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
