package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.utils.DelayedRun
import net.minecraft.client.Minecraft

object MouseCompat {
    var deltaMouseY = 0.0
    var deltaMouseX = 0.0
    var scroll = 0.0
    var timeDelta = 0.0
    val buttonStates = BooleanArray(6) { false }

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
}
