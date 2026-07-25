package at.hannibal2.skyhanni.features.chat.filter

@Suppress("unused")
object MiningChatFilter {
    private val patternGroup = ChatFilterManager.chatFilterGroup.group("mining")
    private val generalConfig get() = ChatFilterManager.generalConfig

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
