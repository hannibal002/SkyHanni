package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * Fires once per second, to enable the [EntityTransparencyTickEvent].
 */
@PrimaryFunction("onEntityTransparencyFeatureActive")
class EntityTransparencyFeatureActiveEvent : SkyHanniEvent() {
    private var status = false

    fun setActive(status: Boolean = true) {
        if (status) {
            this.status = true
        }
    }

    fun isActive() = status
}
