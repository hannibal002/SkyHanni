package at.hannibal2.skyhanni.features.chat.filter

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.utils.IslandDetector

object HuntingChatFilter {
    private val patternGroup = CoreChatFilter.chatFilterGroup.group("hunting")
    private val config get() = CoreChatFilter.config.hunting

    val islandDetector = IslandDetector(
        island = IslandType.GALATEA,
        onIslandJoin = { CoreChatFilter.add(filters) },
        onIslandLeave = { CoreChatFilter.remove(filters) }
    )

    val filters =
        setOf<ChatFilter>(
            RedundantShardsFilter,
            SwoopAxeFilter,
        )

    object RedundantShardsFilter : RegexChatFilter("redundant_shards") {
        override fun isEnabled(): Boolean = config.redundantComments

        /**
         * REGEX-TEST: Mochibear ate too much and passed out! You caught it!
         * REGEX-TEST: You caught yourself an invisibug! The shard was sent to your Hunting Box!
         * REGEX-TEST: The Frog is exhausted...
         */
        override val patterns by patternGroup.list(
            "redundant-comments",
            "Mochibear ate too much and passed out! You caught it!",
            "You caught yourself an invisibug! The shard was sent to your Hunting Box!",
            "The Frog is exhausted\\.\\.\\.",
        )
    }

    object SwoopAxeFilter : RegexChatFilter("swoop_axe") {
        override fun isEnabled(): Boolean = config.swoopAxeMessage

        /**
         * REGEX-TEST: [NPC] Swoop: Wow! I forgot to tell you, monsters around here can only take damage from Axes!
         */
        override val patterns by patternGroup.list(
            "swoop-axe-message",
            "\\[NPC] Swoop: Wow! I forgot to tell you, monsters around here can only take damage from Axes!",
        )
    }
}
