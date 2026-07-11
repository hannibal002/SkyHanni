package at.hannibal2.skyhanni.events.combat

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.slayer.SlayerType

/**
 * Fired when any slayer boss spawns or dies. Can be your own or another player's boss.
 */
sealed class OtherPlayersSlayerEvent(val slayerType: SlayerType, val tier: Int, val owner: String) : SkyHanniEvent() {
    /** Gets called when a slayer is initially detected. */
    class Spawn(slayerType: SlayerType, tier: Int, owner: String) : OtherPlayersSlayerEvent(slayerType, tier, owner)

    /** Gets called when a slayer dies. */
    class Death(slayerType: SlayerType, tier: Int, owner: String) : OtherPlayersSlayerEvent(slayerType, tier, owner)
}
