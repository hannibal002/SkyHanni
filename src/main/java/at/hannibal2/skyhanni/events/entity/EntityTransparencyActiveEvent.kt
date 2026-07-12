package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.Thread

/**
 * Fires once per second, to enable the [EntityTransparencyTickEvent].
 */
@Thread(RENDER)
class EntityTransparencyActiveEvent : SkyHanniEvent() {
    private var status = false

    fun setActive(status: Boolean = true) {
        if (status) {
            this.status = true
        }
    }

    fun isActive() = status
}
