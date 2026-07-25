package at.hannibal2.skyhanni.features.chat.filter

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

object WinterChatFilter : ChatFilterGroup() {
    private val patternGroup = RepoPattern.group("chat-filter.event")
    private val config get() = SkyHanniMod.feature.chat.filterType

    override val filters: Set<ChatFilter> = setOf(
        WinterIslandFilter,
    )
    override val activation = Activation.Island(IslandType.WINTER)

    object WinterIslandFilter : RegexChatFilter("winter_island", { config.others }) {
        /**
         * REGEX-TEST: ☃ [VIP+] liron150 mounted a Snow Cannon!
         */
        override val patterns by patternGroup.list(
            "winter-island",
            "☃ .* mounted a Snow Cannon!",
        )
    }
}
