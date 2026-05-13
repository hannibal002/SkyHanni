package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.BaitType
import net.minecraft.world.item.ItemStack

class BaitUpdateEvent(val baitType: BaitType?, val amount: Int, val itemStack: ItemStack) : SkyHanniEvent()

