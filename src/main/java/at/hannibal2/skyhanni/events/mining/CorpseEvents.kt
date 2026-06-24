package at.hannibal2.skyhanni.events.mining

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.mining.glacitemineshaft.corpse.CorpseType
import at.hannibal2.skyhanni.utils.LorenzVec

/**
 * Fired when a Mineshaft corpse entity is in line of sight of the player.
 */
class CorpseFoundEvent(
    val corpseType: CorpseType,
    val location: LorenzVec,
    val isLastCorpse: Boolean,
) : SkyHanniEvent()

/**
 * Fired when a Mineshaft corpse is looted, signaled in the chat.
 */
class CorpseLootedEvent(val corpseType: CorpseType, val loot: List<Pair<String, Int>>) : SkyHanniEvent()
