package at.hannibal2.skyhanni.features.chat.filter

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.features.garden.pests.PestApi

object FarmingChatFilter : ChatFilterGroup() {
    private val patternGroup = ChatFilterManager.chatFilterGroup.group("farming")
    private val config get() = ChatFilterManager.config
    private val generalConfig get() = ChatFilterManager.generalConfig

    val gardenDetector = IslandDetector(IslandType.GARDEN)

    override val filters: Set<ChatFilter> = setOf(
        AnitaFortuneFilter,
        MasterChefFilter,
        GardenPestFilter,
    )

    object AnitaFortuneFilter : RegexChatFilter("anita_fortune", generalConfig.hideJacob, gardenDetector) {
        /**
         * REGEX-TEST: [NPC] Jacob: Your Anita's Talisman is giving you +25 Carrot Fortune during the contest!
         */
        @Suppress("MaxLineLength")
        override val patterns by patternGroup.list(
            "jacobevent.accessory",
            "\\[NPC] Jacob: Your Anita's \\w+ is giving you \\+\\d{1,2}${SkyblockStat.FARMING_FORTUNE.hypixelIcon} .+ Fortune during the contest!",
        )
    }

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

    object GardenPestFilter : RegexChatFilter("garden_pest", config.gardenNoPest, gardenDetector) {
        /**
         * REGEX-TEST: [NPC] Jacob: Your garden is free of pests! You will not lose any crops to pests during this contest!
         */
        override val patterns = listOf(PestApi.noPestsChatPattern)
    }
}
