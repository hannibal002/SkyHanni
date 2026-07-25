package at.hannibal2.skyhanni.features.chat.filter

import at.hannibal2.skyhanni.data.IslandTypeTag

@Suppress("unused")
object ForagingChatFilter {
    private val patternGroup = CoreChatFilter.chatFilterGroup.group("foraging")
    private val config get() = CoreChatFilter.config.foraging
    private val generalConfig get() = CoreChatFilter.generalConfig

    private val customTreesDetector = IslandDetector(IslandTypeTag.FORAGING_CUSTOM_TREES,)

    object UnmineableTreeFilter : RegexIslandChatFilter("unmineable_tree", config.unmineable, customTreesDetector) {
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
