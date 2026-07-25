package at.hannibal2.skyhanni.features.chat.filter

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.features.garden.pests.PestApi
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

object GardenChatFilter : ChatFilterGroup() {
    private val patternGroup = RepoPattern.group("chat-filter.farming.garden")
    private val config get() = SkyHanniMod.feature.chat.filterType
    private val generalConfig get() = SkyHanniMod.feature.chat

    override val activation = Activation.Island(IslandType.GARDEN)

    override val filters: Set<ChatFilter> = setOf(
        AnitaFortuneFilter,
        GardenPestFilter,
    )

    object AnitaFortuneFilter : RegexChatFilter("anita_fortune", { generalConfig.hideJacob }) {
        /**
         * REGEX-TEST: [NPC] Jacob: Your Anita's Talisman is giving you +25 Carrot Fortune during the contest!
         */
        @Suppress("MaxLineLength")
        override val patterns by patternGroup.list(
            "jacobevent.accessory",
            "\\[NPC] Jacob: Your Anita's \\w+ is giving you \\+\\d{1,2}${SkyblockStat.FARMING_FORTUNE.hypixelIcon} .+ Fortune during the contest!",
        )
    }

    object GardenPestFilter : RegexChatFilter("garden_pest", { config.gardenNoPest }) {
        /**
         * REGEX-TEST: [NPC] Jacob: Your garden is free of pests! You will not lose any crops to pests during this contest!
         */
        override val patterns = listOf(PestApi.noPestsChatPattern)
    }
}
