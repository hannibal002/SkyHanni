package at.hannibal2.skyhanni.features.mining.eventtracker

import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

enum class MiningEvent(val eventName: String, val defaultLength: Duration, private val colourCode: Char) {
    GONE_WITH_THE_WIND("GONE WITH THE WIND", 18.minutes, '9'),
    DOUBLE_POWDER("2X POWDER", 15.minutes, 'b'),
    GOBLIN_RAID("GOBLIN RAID", 5.minutes, 'c'),
    BETTER_TOGETHER("BETTER TOGETHER", 18.minutes, 'd'),
    RAFFLE("RAFFLE", 160.seconds, '6'),
    MITHRIL_GOURMAND("MITHRIL GOURMAND", 10.minutes, 'b'),
    ;

    override fun toString(): String {
        return "§$colourCode$eventName"
    }

    companion object {
        fun fromBossbarName(bossbarName: String): MiningEvent? {
            return MiningEvent.entries.find { it.eventName == bossbarName.removeColor() }
        }
    }
}
