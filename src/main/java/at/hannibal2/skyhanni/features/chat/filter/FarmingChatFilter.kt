package at.hannibal2.skyhanni.features.chat.filter

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

object FarmingChatFilter : ChatFilterGroup() {
    private val patternGroup = RepoPattern.group("chat-filter.farming")
    private val config get() = SkyHanniMod.feature.chat.filterType

    override val filters: Set<ChatFilter> = setOf(
        MasterChefFilter,
    )

    object MasterChefFilter : RegexChatFilter("master_chef", config.masterChef) {
        /**
         * REGEX-TEST: [NPC] Feast Chef Ted: Thanks for the donation! I've added a Kernel to your purse.
         */
        override val patterns by patternGroup.list(
            "master-chef",
            "\\[NPC] Feast Chef Ted: Thanks for the donation! I've added a Kernel to your purse.",
            "\\[NPC] Feast Chef Ted: Thanks for the donation! I've added a Kernel to your purse.",
        )
    }
}
