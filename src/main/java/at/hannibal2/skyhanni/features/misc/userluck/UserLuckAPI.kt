package at.hannibal2.skyhanni.features.misc.userluck

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.features.misc.userluck.UserLuckType.Companion.entries
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object UserLuckAPI {
    val luck get() = UserLuckMultiplier.totalLuckAfterBonus(UserLuckType.getTotalLuck())

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        println(entries)
        event.title("UserLuckAPI")
        try {
            event.addIrrelevant {
                add("UserLuckTypes")
                add("")
                for (type in entries) {
                    add(type.prettyName)
                    add("luck: ${type.luck}")
                    add("")
                }
            }
        } catch(e: Throwable) {
            event.addData("help!!")
            e.printStackTrace()
        }
    }
}
