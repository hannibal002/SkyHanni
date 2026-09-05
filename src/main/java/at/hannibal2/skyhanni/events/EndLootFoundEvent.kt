package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.NeuInternalName

/**
 * Sent for every drop that appears after an End boss died, read from its floating item label
 * rather than from picking it up. Fires only the first time a given drop is seen.
 */
@PrimaryFunction("onEndLootFound")
class EndLootFoundEvent(
    val boss: EndBoss,
    val internalName: NeuInternalName,
    val amount: Int,
) : SkyHanniEvent()
