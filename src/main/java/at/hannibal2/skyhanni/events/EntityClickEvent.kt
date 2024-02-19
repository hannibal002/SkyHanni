package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.ClickType
import net.minecraft.entity.Entity
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.Cancelable

@Cancelable
class EntityClickEvent(clickType: ClickType, val clickedEntity: Entity?, itemInHand: ItemStack?) : WorldClickEvent(itemInHand, clickType)
