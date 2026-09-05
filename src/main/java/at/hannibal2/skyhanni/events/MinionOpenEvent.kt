package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SafeItemStack

@PrimaryFunction("onMinionOpen")
class MinionOpenEvent(val inventoryName: String, val inventoryItems: Map<Int, SafeItemStack>) : SkyHanniEvent()

@PrimaryFunction("onMinionClose")
class MinionCloseEvent : SkyHanniEvent()

@PrimaryFunction("onMinionStorageOpen")
class MinionStorageOpenEvent(val position: LorenzVec?, val inventoryItems: Map<Int, SafeItemStack>) : SkyHanniEvent()
