package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.NeuInternalName

/**
 * Sent for every drop that appears after an End boss died. Read off the armor stand the
 * drop hangs on, so it arrives before anything is picked up. Either way, each drop is only sent once.
 */
@PrimaryFunction("onEndLootFound")
class EndLootFoundEvent(
    val boss: EndBoss,
    val internalName: NeuInternalName,
    val amount: Int,
) : SkyHanniEvent()
