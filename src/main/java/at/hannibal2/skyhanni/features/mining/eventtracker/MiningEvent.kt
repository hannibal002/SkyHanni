package at.hannibal2.skyhanni.features.mining.eventtracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

enum class MiningEvent(
    val eventName: String,
    private val shortName: String,
    val defaultLength: Duration,
    private val colourCode: Char
) {
    GONE_WITH_THE_WIND("GONE WITH THE WIND", "Wind", 18.minutes, '9'),
    DOUBLE_POWDER("2X POWDER", "2x", 15.minutes, 'b'),
    GOBLIN_RAID("GOBLIN RAID", "Raid", 5.minutes, 'c'),
    BETTER_TOGETHER("BETTER TOGETHER", "Better", 18.minutes, 'd'),
    RAFFLE("RAFFLE", "Raffle", 160.seconds, '6'),
    MITHRIL_GOURMAND("MITHRIL GOURMAND", "Gourmand", 10.minutes, 'b'),
    ;

    private val config get() = SkyHanniMod.feature.mining.miningEvent

    override fun toString(): String {
        return if (config.compressedFormat) "§$colourCode$shortName" else "§$colourCode$eventName"
    }

    fun toPastString(): String {
        return if (config.compressedFormat) "§7$shortName" else "§7$eventName"
    }

    companion object {
        fun fromBossbarName(bossbarName: String): MiningEvent? {
            return MiningEvent.entries.find { it.eventName == bossbarName.removeColor() }
        }
    }
}
