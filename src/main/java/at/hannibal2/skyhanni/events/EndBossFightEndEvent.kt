package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/** The boss of an End island fight that ended. Both share the same end-of-fight summary format. */
enum class EndBoss {
    DRAGON,
    END_STONE_PROTECTOR,
}

/**
 * Sent once the end-of-fight summary of a dragon or End Stone Protector fight has been read
 * completely. Carries the raw fight result so features like weight calculation or loot tracking
 * do not have to parse the chat themselves.
 */
@PrimaryFunction("onEndBossFightEnd")
class EndBossFightEndEvent(
    val boss: EndBoss,
    /** Own placement on the damage leaderboard, 1-based. */
    val place: Int,
    val yourDamage: Double,
    /** Damage of the first place, or 0.0 when it could not be read. */
    val topDamage: Double,
) : SkyHanniEvent()
