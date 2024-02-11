package at.hannibal2.skyhanni.events.item

import at.hannibal2.skyhanni.events.LorenzEvent
import at.hannibal2.skyhanni.features.itemabilities.abilitycooldown.ItemAbilityType
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.Cancelable

@Cancelable
class ItemAbilityCastEvent(val itemAbility: ItemAbilityType, val itemStack: ItemStack): LorenzEvent()
