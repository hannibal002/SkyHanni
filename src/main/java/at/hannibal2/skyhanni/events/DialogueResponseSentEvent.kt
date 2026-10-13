package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * Fired when the player clicks on any chat component with a custom click action dedicated to skyblock dialogue responses.
 * This is handled by [at.hannibal2.skyhanni.api.NpcApi] by filtering the outgoing packets.
 *
 * This event originates from the network thread.
 *
 * @param npcId The id of the npc that the dialogue was sent for.
 * @param responseKey The key for the response that the user selected (e.g. r_2_1).
 */
@PrimaryFunction("onDialogueResponseSent")
class DialogueResponseSentEvent(val npcId: String, val responseKey: String) : CancellableSkyHanniEvent()
