package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

class PurseChangeEvent(val coins: Double, val purse: Double, val reason: PurseChangeCause) : SkyHanniEvent()

enum class PurseChangeCause {
    GAIN_MOB_KILL,
    GAIN_TALISMAN_OF_COINS,
    GAIN_DICE_ROLL_ARCHFIEND,
    GAIN_DICE_ROLL_HIGHCLASS,
    GAIN_UNKNOWN,

    LOSE_SLAYER_QUEST_STARTED,
    LOSE_DICE_ROLL_COST_ARCHFIEND,
    LOSE_DICE_ROLL_COST_HIGHCLASS,
    LOSE_UNKNOWN,
}
