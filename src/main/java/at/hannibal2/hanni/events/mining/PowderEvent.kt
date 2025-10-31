package at.hannibal2.hanni.events.mining

import at.hannibal2.hanni.api.HotmApi
import at.hannibal2.hanni.api.event.HanniEvent

open class PowderEvent(val powder: HotmApi.PowderType) : HanniEvent() {
    class Gain(powder: HotmApi.PowderType, val amount: Long) : PowderEvent(powder)
    class Spent(powder: HotmApi.PowderType, val amount: Long) : PowderEvent(powder)
}
