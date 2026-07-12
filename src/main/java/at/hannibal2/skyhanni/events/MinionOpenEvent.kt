package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.Thread
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SafeItemStack

@Thread(NETWORK, RENDER)
class MinionOpenEvent(val inventoryName: String, val inventoryItems: Map<Int, SafeItemStack>) : SkyHanniEvent()
@Thread(RENDER)
class MinionCloseEvent : SkyHanniEvent()
@Thread(NETWORK)
class MinionStorageOpenEvent(val position: LorenzVec?, val inventoryItems: Map<Int, SafeItemStack>) : SkyHanniEvent()
