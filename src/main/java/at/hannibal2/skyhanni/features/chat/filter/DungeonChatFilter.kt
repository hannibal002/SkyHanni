package at.hannibal2.skyhanni.features.chat.filter

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.IslandDetector

@SkyHanniModule
object DungeonChatFilter {
    private val patternGroup = CoreChatFilter.chatFilterGroup.group("dungeon")
    private val config get() = SkyHanniMod.feature.dungeon.messageFilter

    val islandDetector = IslandDetector(
        island = IslandType.CATACOMBS,
        onIslandJoin = { CoreChatFilter.add(filters) },
        onIslandLeave = { CoreChatFilter.remove(filters) }
    )

    val filters =
        setOf<ChatFilter>(
            RareDropsFilter,
            SoloClassFilter,
            SoloStatsFilter,
            FairyFilter,
        )

    object RareDropsFilter : RegexChatFilter("rare_drops") {
        override fun isEnabled(): Boolean = config.rareDrops

        /**
         * REGEX-TEST: RARE REWARD! Leebys found a Recombobulator 3000 in their Obsidian Chest!
         */
        override val patterns by patternGroup.list(
            "rare-drops",
            "RARE REWARD! .* found a .* in their .* Chest!",
        )
    }


    object SoloClassFilter : RegexChatFilter("solo_class") {
        override fun isEnabled(): Boolean = config.soloClass

        /**
         * REGEX-TEST: Your Healer stats are doubled because you are the only player using this object!
         * REGEX-TEST: Your Mage stats are doubled because you are the only player using this object!
         */
        override val patterns by patternGroup.list(
            "solo-object",
            "Your (?:Healer|Mage|Berserk|Archer|Tank) stats are doubled because you are the only player using this object!",
        )
    }


    object SoloStatsFilter : RegexChatFilter("solo_stats") {
        override fun isEnabled(): Boolean = config.soloStats

        /**
         * REGEX-TEST: [Healer] My not know this one TODO: this one
         */
        override val patterns by patternGroup.list(
            "solo-stats",
            "\\[(?:Healer|Mage|Berserk|Archer|Tank)].*",
        )
    }

    object FairyFilter : RegexChatFilter("fairy") {
        override fun isEnabled(): Boolean = config.fairy

        /**
         * REGEX-TEST: Genevieve the Fairy: You killed me! Take this Revive Stone so that my death is not in vain!
         */
        override val patterns by patternGroup.list(
            "fairy",
            "[\\w']+ the Fairy: You killed me! Take this Revive Stone so that my death is not in vain!",
            "[\\w']+ the Fairy: You killed me! I'll revive you so that my death is not in vain!",
            "[\\w']+ the Fairy: Have a great life!",
        )
    }
}
