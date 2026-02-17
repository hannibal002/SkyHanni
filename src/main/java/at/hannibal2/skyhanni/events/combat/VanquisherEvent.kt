package at.hannibal2.skyhanni.events.combat

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.combat.crimsonisle.VanquisherApi

sealed class VanquisherEvent : SkyHanniEvent() {
    class DeSpawn(val data: VanquisherApi.VanquisherData) : VanquisherEvent()
    class Death(val data: VanquisherApi.VanquisherData) : VanquisherEvent()
    class Spawn(val data: VanquisherApi.VanquisherData) : VanquisherEvent()
    data object OwnSpawn : VanquisherEvent()
}
