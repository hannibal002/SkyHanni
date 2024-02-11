package at.hannibal2.skyhanni.events.item

import at.hannibal2.skyhanni.events.LorenzEvent
import at.hannibal2.skyhanni.features.itemabilities.abilitycooldown.ItemAbility
import net.minecraftforge.fml.common.eventhandler.Cancelable

@Cancelable
class ItemAbilityCastEvent(val itemAbility: ItemAbility): LorenzEvent()
