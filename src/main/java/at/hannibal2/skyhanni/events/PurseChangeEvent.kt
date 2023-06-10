package at.hannibal2.skyhanni.events

class PurseChangeEvent(val coins: Double, val reason: PurseChangeCause) : LorenzEvent()

enum class PurseChangeCause {
    GAIN_MOB_KILL,
    GAIN_TALISMAN_OF_COINS,
    GAIN_UNKNOWN,

    LOSE_SLAYER_QUEST_STARTED,
    LOSE_UNKNOWN,
}