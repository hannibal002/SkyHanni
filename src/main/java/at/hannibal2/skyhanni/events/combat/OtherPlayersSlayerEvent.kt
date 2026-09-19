package at.hannibal2.skyhanni.events.combat

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.slayer.SlayerType

/**
 * Fired when any player's slayer boss spawns or dies.
 *
 * [slayer] holds the type of slayer boss
 * [tier] holds the tier of the boss
 * [owner] holds the username of the player who spawned the boss
 */
sealed class OtherPlayersSlayerEvent(val slayer: SlayerType, val tier: Int, val owner: String) : SkyHanniEvent() {
    /** Gets called when a slayer is initially detected. */
    class Spawn(slayerType: SlayerType, tier: Int, owner: String) : OtherPlayersSlayerEvent(slayerType, tier, owner)

    /** Gets called when a slayer dies. */
    class Death(slayerType: SlayerType, tier: Int, owner: String) : OtherPlayersSlayerEvent(slayerType, tier, owner)
}
