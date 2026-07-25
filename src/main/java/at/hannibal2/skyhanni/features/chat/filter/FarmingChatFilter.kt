package at.hannibal2.skyhanni.features.chat.filter

object FarmingChatFilter : ChatFilterGroup() {
    private val patternGroup = ChatFilterManager.chatFilterGroup.group("farming")
    private val config get() = ChatFilterManager.config

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
