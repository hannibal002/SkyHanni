package at.hannibal2.skyhanni.events.combat

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.nether.CrimsonMinibossRespawnTimer.MiniBoss

/** Gets called when a Crimson Isle miniboss is killed and the user dealt damage to it. */
class CrimsonMinibossKilledEvent(val miniboss: MiniBoss) : SkyHanniEvent()
