package at.hannibal2.skyhanni.features.chat.filter

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

object ForagingChatFilter : ChatFilterGroup() {
    private val patternGroup = RepoPattern.group("chat-filter.foraging")
    private val config get() = SkyHanniMod.feature.chat.filterType.foraging
    private val generalConfig get() = SkyHanniMod.feature.chat

    private val customTreesDetector = IslandDetector(IslandTypeTag.FORAGING_CUSTOM_TREES)

    override val filters: Set<ChatFilter> = setOf(
        UnmineableTreeFilter,
        LotteryFilter,
    )

    object UnmineableTreeFilter : RegexChatFilter("unmineable_tree", config.unmineable, customTreesDetector) {
        /**
         ** REGEX-TEST: You cannot damage a tree while it is regenerating!
         ** REGEX-TEST: The toughness of this tree is way too high!
         */
        override val patterns by patternGroup.list(
            "unmineable-tree",
            "You cannot damage a tree while it is regenerating!",
            "The toughness of this tree is way too high!",
        )
    }

    object LotteryFilter : RegexChatFilter("lottery", generalConfig.hideLottery) {
        /**
         ** REGEX-TEST: New day! Your Lottery buff changed!
         */
        override val patterns by patternGroup.list(
            "lottery",
            "New day! Your Lottery buff changed!",
        )
    }
}
