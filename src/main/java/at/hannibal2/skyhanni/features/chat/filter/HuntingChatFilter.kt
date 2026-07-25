package at.hannibal2.skyhanni.features.chat.filter

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

object HuntingChatFilter : ChatFilterGroup() {
    private val patternGroup = RepoPattern.group("chat-filter.hunting")
    private val config get() = SkyHanniMod.feature.chat.filterType.hunting

    override val activation: ChatFilterActivation = ChatFilterActivation.Island(IslandTypeTag.FORAGING_CUSTOM_TREES)

    override val filters: Set<ChatFilter> = setOf(
        RedundantShardsFilter,
        SwoopAxeFilter,
    )

    object RedundantShardsFilter : RegexChatFilter("redundant_shards", { config.redundantComments }) {
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

    object SwoopAxeFilter : RegexChatFilter("swoop_axe", { config.swoopAxeMessage }) {
        /**
         * REGEX-TEST: [NPC] Swoop: Wow! I forgot to tell you, monsters around here can only take damage from Axes!
         */
        override val patterns by patternGroup.list(
            "swoop-axe-message",
            "\\[NPC] Swoop: Wow! I forgot to tell you, monsters around here can only take damage from Axes!",
        )
    }
}
