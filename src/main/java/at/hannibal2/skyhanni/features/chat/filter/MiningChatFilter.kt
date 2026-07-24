package at.hannibal2.skyhanni.features.chat.filter

object MiningChatFilter {
    private val patternGroup = CoreChatFilter.chatFilterGroup.group("mining")
    private val generalConfig get() = CoreChatFilter.generalConfig

    val filters =
        setOf<ChatFilter>(
            SkymallFilter,
        )

    init {
        CoreChatFilter.add(filters)
    }

    object SkymallFilter : RegexChatFilter("skymall") {
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
