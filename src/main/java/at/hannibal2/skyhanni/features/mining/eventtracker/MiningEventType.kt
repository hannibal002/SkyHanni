package at.hannibal2.skyhanni.features.mining.eventtracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

enum class MiningEventType(
    val eventName: String,
    private val shortName: String,
    val defaultLength: Duration,
    private val colourCode: Char,
    val dwarvenSpecific: Boolean,
) {
    GONE_WITH_THE_WIND("GONE WITH THE WIND", "Wind", 18.minutes, '9', false),
    DOUBLE_POWDER("2X POWDER", "2x", 15.minutes, 'b', false),
    GOBLIN_RAID("GOBLIN RAID", "Raid", 5.minutes, 'c', true),
    BETTER_TOGETHER("BETTER TOGETHER", "Better", 18.minutes, 'd', false),
    RAFFLE("RAFFLE", "Raffle", 160.seconds, '6', true),
    MITHRIL_GOURMAND("MITHRIL GOURMAND", "Gourmand", 10.minutes, 'b', true),
    ;

    override fun toString() =
        if (config.compressedFormat) "§$colourCode$shortName" else "§$colourCode$eventName"

    fun toPastString() =
        if (config.compressedFormat) "§7$shortName" else "§7$eventName"

    companion object {
        private val config get() = SkyHanniMod.feature.mining.miningEvent

        fun fromEventName(bossbarName: String): MiningEventType? {
            return MiningEventType.entries.find { it.eventName == bossbarName.removeColor() }
        }
    }
}
