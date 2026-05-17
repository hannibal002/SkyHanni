package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import at.hannibal2.skyhanni.data.InteractClickType
import at.hannibal2.skyhanni.utils.SafeItemStack
import net.minecraft.world.item.ItemStack

open class WorldClickEvent(val itemInHand: SafeItemStack?, val clickType: InteractClickType) : CancellableSkyHanniEvent()
