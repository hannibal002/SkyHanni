package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.utils.SafeItemStack

open class WorldClickEvent(val itemInHand: SafeItemStack?, val clickType: ClickType) : CancellableSkyHanniEvent()
