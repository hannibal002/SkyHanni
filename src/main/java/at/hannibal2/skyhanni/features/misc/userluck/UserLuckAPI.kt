package at.hannibal2.skyhanni.features.misc.userluck

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object UserLuckAPI {
    val luck get() = UserLuckMultiplier.totalLuckAfterBonus(UserLuckType.totalLuck)

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("UserLuckAPI")
        event.addIrrelevant {
            add("UserLuckTypes")
            add("")
            for (type in UserLuckType.entries) {
                add("${type.prettyName}: ${type.luck}")
            }
            add("")
            add("UserLuckMultiplier")
            add("")
            for (type in UserLuckMultiplier.entries) {
                add("${type.name}: ${type.condition.invoke()}${if (type.isMultiplicative) " | isMultiplicative" else ""}")
            }
        }
    }
}
