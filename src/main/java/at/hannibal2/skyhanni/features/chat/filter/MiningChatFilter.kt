package at.hannibal2.skyhanni.features.chat.filter

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

object MiningChatFilter : ChatFilterGroup() {
    private val patternGroup = RepoPattern.group("chat-filter.mining")
    private val generalConfig get() = SkyHanniMod.feature.chat

    override val filters: Set<ChatFilter> = setOf(
        SkymallFilter,
    )

    object SkymallFilter : RegexChatFilter("skymall", generalConfig.hideSkyMall) {
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
