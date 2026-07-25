package at.hannibal2.skyhanni.features.chat.filter

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.features.garden.pests.PestApi

@Suppress("unused")
object FarmingChatFilter {
    private val patternGroup = CoreChatFilter.chatFilterGroup.group("farming")
    private val config get() = CoreChatFilter.config
    private val generalConfig get() = CoreChatFilter.generalConfig

    val gardenDetector = IslandDetector(IslandType.GARDEN)

    object AnitaFortuneFilter : RegexIslandChatFilter("anita_fortune", generalConfig.hideJacob, gardenDetector) {
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

    object GardenPestFilter : RegexIslandChatFilter("garden_pest", config.gardenNoPest, gardenDetector) {
        /**
         * REGEX-TEST: [NPC] Jacob: Your garden is free of pests! You will not lose any crops to pests during this contest!
         */
        override val patterns = listOf(PestApi.noPestsChatPattern)
    }
}
