package at.hannibal2.skyhanni.events.entity.slayer

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.slayer.SlayerType

@Deprecated(
    "use OtherPlayersSlayerEvent.Death instead",
    ReplaceWith(
        "OtherPlayersSlayerEvent.Death",
        "at.hannibal2.skyhanni.events.combat.OtherPlayersSlayerEvent",
    ),
)
class SlayerDeathEvent(val slayerType: SlayerType, val tier: Int, val owner: String?) : SkyHanniEvent()
