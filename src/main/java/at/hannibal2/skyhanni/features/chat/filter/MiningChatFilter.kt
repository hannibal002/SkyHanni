package at.hannibal2.skyhanni.features.chat.filter

import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.utils.IslandDetector

object MiningChatFilter {
    private val patternGroup = ChatFilter.chatFilterGroup.group("mining")
    private val generalConfig get() = ChatFilter.generalConfig

    val islandDetector = IslandDetector(
        islandTag = IslandTypeTag.MINING,
        onIslandJoin = { CoreChatFilter.add(filters) },
        onIslandLeave = { CoreChatFilter.remove(filters) }
    )

    val filters =
        setOf<ChatFilter>(
            SkymallFilter(),
        )

    class SkymallFilter : RegexChatFilter("skymall") {
        override fun isEnabled(): Boolean = generalConfig.hideSkyMall

        /**
         * REGEX-TEST: New day! Your Sky Mall buff changed!
         * REGEX-TEST: You can disable this messaging by toggling Sky Mall in your /hotm!
         */
        override val patterns by patternGroup.list(
            "skymall",
            "New day! Your Sky Mall buff changed!",
            "You can disable this messaging by toggling Sky Mall in your /hotm!",
        )
    }
}
