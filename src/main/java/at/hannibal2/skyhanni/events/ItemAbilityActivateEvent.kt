package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.itemabilities.abilitycooldown.ItemAbility
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * Fired when an item ability is activated.
 *
 * @param ability the activated item ability
 */
@PrimaryFunction("onItemAbilityActivate")
class ItemAbilityActivateEvent(val ability: ItemAbility): SkyHanniEvent()
