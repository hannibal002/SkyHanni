package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

/**
 * Fires once per second, to enable the [EntityOpacityEvent].
 */
class EntityOpacityActiveEvent : SkyHanniEvent() {
    private var status = false

    fun setActive(status: Boolean = true) {
        if (status) {
            this.status = true
        }
    }

    fun isActive() = status
}
