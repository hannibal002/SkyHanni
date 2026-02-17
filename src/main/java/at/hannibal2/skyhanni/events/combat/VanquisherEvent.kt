package at.hannibal2.skyhanni.events.combat

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.combat.crimsonisle.VanquisherAPI

sealed class VanquisherEvent : SkyHanniEvent() {
    class DeSpawn(val data: VanquisherAPI.VanquisherData) : VanquisherEvent()
    class Death(val data: VanquisherAPI.VanquisherData) : VanquisherEvent()
    class Spawn(val data: VanquisherAPI.VanquisherData) : VanquisherEvent()
    data object OwnSpawn : VanquisherEvent()
}
