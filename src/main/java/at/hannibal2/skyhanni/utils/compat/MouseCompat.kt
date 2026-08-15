package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.events.minecraft.KeyDownEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.utils.DelayedRun
import net.minecraft.client.Minecraft
import net.minecraft.client.input.MouseButtonInfo
import kotlin.math.sign

/**
 * This is a compatibility layer that helps with multiple Minecraft versions and mixins.
 * This class should be used in utils/data/api classes and not in feature classes.
 */
object MouseCompat {

    const val NUMBER_OF_MOUSE_BUTTONS = 6

    @JvmStatic
    var deltaMouseX = 0.0

    @JvmStatic
    var deltaMouseY = 0.0
        set(value) {
            field = value
            mouseMoveEventId++
        }

    @JvmStatic
    var scroll = 0.0
        set(value) {
            field = value
            if (value != 0.0) scrollEventId++
        }

    private var mouseMoveEventId = 0L
    private var scrollEventId = 0L

    private val buttonStates = BooleanArray(NUMBER_OF_MOUSE_BUTTONS)

    private val mouse by lazy { Minecraft.getInstance().mouseHandler }

    fun isMouseButton(button: Int) = button in 0..5

    fun isButtonDown(button: Int): Boolean {
        if (isMouseButton(button)) return buttonStates[button]
        return false
    }

    fun setButtonState(button: Int, down: Boolean) {
        if (isMouseButton(button)) {
            buttonStates[button] = down
        }
    }

    fun getScrollDelta(): Int {
        return (getPreciseScrollDelta() * 120).toInt()
    }

    fun getPreciseScrollDelta(): Double {
        val delta = scroll
        DelayedRun.runNextTickEnd { scroll = 0.0 }
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

    fun getEventButtonState(): Boolean = buttonStates.any { it }

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
