package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.utils.LorenzVec
import net.minecraft.item.ItemStack

class MinionOpenEvent(val inventoryName: String, val inventoryItems: Map<Int, ItemStack>) : HanniEvent()
class MinionCloseEvent : HanniEvent()
class MinionStorageOpenEvent(val position: LorenzVec?, val inventoryItems: Map<Int, ItemStack>) : HanniEvent()
