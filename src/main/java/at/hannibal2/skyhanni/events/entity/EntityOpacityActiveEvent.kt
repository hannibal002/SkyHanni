package at.hannibal2.hanni.events.entity

import at.hannibal2.hanni.api.event.HanniEvent

/**
 * Fires once per second, to enable the [EntityOpacityEvent].
 */
class EntityOpacityActiveEvent : HanniEvent() {
    private var status = false

    fun setActive(status: Boolean = true) {
        if (status) {
            this.status = true
        }
    }

    fun isActive() = status
}
