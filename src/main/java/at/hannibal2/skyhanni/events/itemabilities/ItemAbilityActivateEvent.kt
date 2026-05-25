package at.hannibal2.skyhanni.events.itemabilities

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.itemabilities.abilitycooldown.ItemAbility
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

@PrimaryFunction("onItemAbilityActivate")
class ItemAbilityActivateEvent(val ability: ItemAbility) : SkyHanniEvent()
