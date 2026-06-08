package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

@PrimaryFunction("onFov")
class FovEvent(private var fov: Int): SkyHanniEvent() {
    private var additive = 0.0
    private var multiplier = 1.0

    fun add(value: Double) {
        additive += value
    }

    fun mult(factor: Double) {
        multiplier *= factor
    }

    fun setFov(targetFov: Int) {
        fov = targetFov
    }

    fun getResult(): Int {
        return ((fov + additive) * multiplier).toInt()
    }
}
