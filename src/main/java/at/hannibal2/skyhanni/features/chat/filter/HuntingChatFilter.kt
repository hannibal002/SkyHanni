package at.hannibal2.skyhanni.features.chat.filter

import at.hannibal2.skyhanni.data.IslandType

object HuntingChatFilter : ChatFilterGroup {
    private val patternGroup = ChatFilterManager.chatFilterGroup.group("hunting")
    private val config get() = ChatFilterManager.config.hunting

    val galateaDetector = IslandDetector(island = IslandType.GALATEA)

    override val filters: Set<ChatFilter> = setOf(
        RedundantShardsFilter,
        SwoopAxeFilter,
    )

    object RedundantShardsFilter : RegexIslandChatFilter("redundant_shards", config.redundantComments, galateaDetector) {
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

    object SwoopAxeFilter : RegexIslandChatFilter("swoop_axe", config.swoopAxeMessage, galateaDetector) {
        /**
         * REGEX-TEST: [NPC] Swoop: Wow! I forgot to tell you, monsters around here can only take damage from Axes!
         */
        override val patterns by patternGroup.list(
            "swoop-axe-message",
            "\\[NPC] Swoop: Wow! I forgot to tell you, monsters around here can only take damage from Axes!",
        )
    }
}
