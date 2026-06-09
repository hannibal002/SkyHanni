package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

@PrimaryFunction("onFov")
class FovEvent(private var fov: Int) : SkyHanniEvent() {
    private var multiplier = 1.0
    private var currentPriority = 1000

    fun mult(factor: Double) {
        multiplier *= factor
    }

    fun setFov(targetFov: Int, priority: Int = 1000) {
        if (currentPriority > priority) return
        fov = targetFov
        currentPriority = priority
    }

    fun getResult(): Int {
        return (fov * multiplier).toInt()
    }
}
