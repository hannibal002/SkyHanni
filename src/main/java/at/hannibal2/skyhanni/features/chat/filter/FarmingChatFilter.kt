package at.hannibal2.skyhanni.features.chat.filter

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.features.garden.pests.PestApi
import at.hannibal2.skyhanni.utils.IslandDetector

object FarmingChatFilter {
    private val patternGroup = ChatFilter.chatFilterGroup.group("farming")
    private val config get() = ChatFilter.config
    private val generalConfig get() = ChatFilter.generalConfig

    private val islandDetector =
        IslandDetector(
            island = IslandType.GARDEN,
            onIslandJoin = { CoreChatFilter.add(gardenFilters) },
            onIslandLeave = { CoreChatFilter.remove(gardenFilters) }
        )

    val gardenFilters =
        setOf<ChatFilter>(
            AnitaFortuneFilter,
            GardenPestFilter
        )

    val filters =
        setOf<ChatFilter>(
            MasterChefFilter,
        )

    init {
        CoreChatFilter.add(filters)
    }

    object AnitaFortuneFilter : RegexChatFilter("anita_fortune") {
        override fun isEnabled(): Boolean = generalConfig.hideJacob

        /**
         * REGEX-TEST: [NPC] Jacob: Your Anita's Talisman is giving you +25 Carrot Fortune during the contest!
         */
        @Suppress("MaxLineLength")
        override val patterns by patternGroup.list(
            "jacobevent.accessory",
            "\\[NPC] Jacob: Your Anita's \\w+ is giving you \\+\\d{1,2}${SkyblockStat.FARMING_FORTUNE.hypixelIcon} .+ Fortune during the contest!",
        )
    }

    object MasterChefFilter : RegexChatFilter("master_chef") {
        override fun isEnabled(): Boolean = config.masterChef

        /**
         * REGEX-TEST: [NPC] Feast Chef Ted: Thanks for the donation! I've added a Kernel to your purse.
         */
        override val patterns by patternGroup.list(
            "master-chef",
            "\\[NPC] Feast Chef Ted: Thanks for the donation! I've added a Kernel to your purse.",
            "\\[NPC] Feast Chef Ted: Thanks for the donation! I've added a Kernel to your purse.",
        )
    }

    object GardenPestFilter : RegexChatFilter("garden_pest") {
        override fun isEnabled(): Boolean = config.gardenNoPest

        /**
         * REGEX-TEST: [NPC] Jacob: Your garden is free of pests! You will not lose any crops to pests during this contest!
         */
        override val patterns = listOf(PestApi.noPestsChatPattern)
    }
}
