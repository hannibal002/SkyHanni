package at.hannibal2.skyhanni.events.inventory

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.LoreCostUtils
import at.hannibal2.skyhanni.utils.NeuInternalName

/**
 * Fired when the player bought something in an npc trade menu, as soon as the server confirms
 * the purchase in chat.
 *
 * [internalName] is the bought item and [amount] how many of it were bought at once. [costs] is
 * what a single one of them costs, exactly as the item lore lists it, so multiply the two to get
 * what the purchase took in total.
 *
 * Only fired while the menu the item was bought from is still known, a purchase from an already
 * closed menu cannot be priced and stays silent.
 */
@PrimaryFunction("onNpcTrade")
class NpcTradeEvent(
    val internalName: NeuInternalName,
    val amount: Long,
    val costs: List<LoreCostUtils.LoreCostEntry>,
) : SkyHanniEvent()
