package at.hannibal2.skyhanni.events.mining

import at.hannibal2.skyhanni.api.HotmApi
import at.hannibal2.skyhanni.api.event.SkyHanniEvent

open class PowderEvent(val powder: HotmApi.PowderType, val amount: Long) : SkyHanniEvent() {
    class Gain(powder: HotmApi.PowderType, amount: Long) : PowderEvent(powder, amount)
    class Spent(powder: HotmApi.PowderType, amount: Long) : PowderEvent(powder, -amount)
}
