package at.hannibal2.skyhanni.features.chat.filter

import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.utils.IslandDetector

object ForagingChatFilter {
    private val patternGroup = ChatFilter.chatFilterGroup.group("foraging")
    private val config get() = ChatFilter.config.foraging
    private val generalConfig get() = ChatFilter.generalConfig

    val customTreesDetector = IslandDetector(
        islandTag = IslandTypeTag.FORAGING_CUSTOM_TREES,
        onIslandJoin = { CoreChatFilter.add(UnmineableTreeFilter) },
        onIslandLeave = { CoreChatFilter.remove(UnmineableTreeFilter) }
    )

    init {
        CoreChatFilter.add(LotteryFilter)
    }

    object UnmineableTreeFilter : RegexChatFilter("unmineable_tree") {
        override fun isEnabled(): Boolean = IslandTypeTag.FORAGING_CUSTOM_TREES.isInIsland() && config.unmineable

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

    object LotteryFilter : RegexChatFilter("lottery") {
        override fun isEnabled(): Boolean = generalConfig.hideLottery

        /**
         ** REGEX-TEST: New day! Your Lottery buff changed!
         */
        override val patterns by patternGroup.list(
            "lottery",
            "New day! Your Lottery buff changed!",
        )
    }
}
