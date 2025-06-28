package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.utils.DelayedRun
import net.minecraft.client.MinecraftClient

object MouseCompat {
    var deltaMouseY = 0.0
    var deltaMouseX = 0.0
    var scroll = 0.0
    var timeDelta = 0.0
    var lastEventButton = -1

    private val mouse by lazy {
        MinecraftClient.getInstance().mouse
    }

    fun isButtonDown(button: Int): Boolean {
        return lastEventButton == button
    }

    fun getScrollDelta(): Int {
        val delta = scroll
        DelayedRun.runNextTick { scroll = 0.0 }
        return delta.toInt() * 120
    }

    fun getX(): Int {
        return mouse.x.toInt()
    }

    fun getY(): Int {
        return mouse.y.toInt()
    }

    // I have no clue what the difference between getx and geteventx is on 1.8.9
    // on 1.8.9 they are pretty much the same (they are the exact same when the mouse is still)
    fun getEventX(): Int = getX()
    fun getEventY(): Int = getY()

    fun getEventButtonState(): Boolean = lastEventButton != -1
    fun getEventNanoseconds(): Long = timeDelta.toLong()

    fun getEventDY(): Int {
        return deltaMouseY.toInt()
    }
}
