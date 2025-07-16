package de.hype.bingonet.shared.constants

import at.hannibal2.skyhanni.features.mining.eventtracker.MiningEventType

// Mining Events

/**
 * A List of all Mining Events
 * [.BETTER_TOGETHER]
 * [.DOUBLE_POWDER]
 * [.GONE_WITH_THE_WIND]
 * [.GOBLIN_RAID]
 * [.MITHRIL_GOURMAND]
 * [.RAFFLE]
 */
enum class MiningEvents(@JvmField val displayName: String) {
    BETTER_TOGETHER("Better Together"),
    DOUBLE_POWDER("Double Powder"),
    GONE_WITH_THE_WIND("Gone with the Wind"),
    GOBLIN_RAID("Goblin Raid"),
    MITHRIL_GOURMAND("Mithril Gourmand"),
    RAFFLE("Raffle");

    val isDWEventOnly: Boolean
        //Some Events can't happen in Crystal Hollows
        get() {
            return this == MITHRIL_GOURMAND || this == RAFFLE || this == GOBLIN_RAID
        }

    companion object {
        @JvmStatic
        fun isDWEventOnly(event: String): Boolean {
            return event == MITHRIL_GOURMAND.displayName || event == RAFFLE.displayName || event == GOBLIN_RAID.displayName
        }

        @JvmStatic
        fun getByDisplayName(string: String): MiningEvents? {
            for (event in entries) {
                if (event.displayName == string) return event
            }
            return null
        }
    }
}
