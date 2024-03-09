package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.ClickType
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.Cancelable

@Cancelable
class ItemClickEvent(itemInHand: ItemStack?, clickType: ClickType) : WorldClickEvent(itemInHand, clickType)
