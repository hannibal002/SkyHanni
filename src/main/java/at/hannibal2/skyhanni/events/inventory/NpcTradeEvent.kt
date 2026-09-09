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
 * The costs come from the last menu that listed the item under that name, and that list is kept
 * after the menu closed: some shops ask for the amount in a follow up menu and only confirm the
 * purchase once the shop itself is already gone.
 */
@PrimaryFunction("onNpcTrade")
class NpcTradeEvent(
    val internalName: NeuInternalName,
    val amount: Long,
    val costs: List<LoreCostUtils.LoreCostEntry>,
) : SkyHanniEvent()
