package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.HanniEvent

class PurseChangeEvent(val coins: Double, val purse: Double, val reason: PurseChangeCause) : HanniEvent()

enum class PurseChangeCause {
    GAIN_MOB_KILL,
    GAIN_TALISMAN_OF_COINS,
    GAIN_DICE_ROLL,
    GAIN_UNKNOWN,

    LOSE_SLAYER_QUEST_STARTED,
    LOSE_DICE_ROLL_COST,
    LOSE_UNKNOWN,
}
