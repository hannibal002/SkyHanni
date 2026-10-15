package at.hannibal2.skyhanni.events.yearofthepig

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.skillprogress.SkillType
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.NeuInternalName

/**
 * Event that is fired when a shiny orb is looted.
 * Additionally, a Shiny Token is always obtained when looting a shiny orb,
 * but this event does not include the shiny token in the rewards list.
 *
 * @property rewards The list of rewards obtained from the shiny orb.
 */
@PrimaryFunction("onShinyOrbLooted")
class ShinyOrbLootedEvent(
    val rewards: List<Reward>,
) : SkyHanniEvent() {

    sealed interface Reward {
        data class Loot(
            val internalName: NeuInternalName,
            val amount: Int,
        ) : Reward

        data class Coins(
            val amount: Int,
        ) : Reward

        data class SkillXp(
            val skill: SkillType,
            val amount: Int,
        ) : Reward
    }
}
