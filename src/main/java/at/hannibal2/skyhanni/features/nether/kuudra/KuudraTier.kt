package at.hannibal2.skyhanni.features.nether.kuudra

import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.KuudraQuest
import at.hannibal2.skyhanni.features.nether.reputationhelper.dailyquest.quest.QuestState
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

enum class KuudraTier(val displayName: String) {
    BASIC("Basic"),
    HOT("Hot"),
    BURNING("Burning"),
    FIERY("Fiery"),
    INFERNAL("Infernal"),
    ;

    var doneToday: Boolean = false

    private var intLocation: LorenzVec? = null
    private var intTierNumber: Int = ordinal + 1
    private var intDisplayItem: NeuInternalName = "KUUDRA_${name}_TIER_KEY".toInternalName()

    val location: LorenzVec? get() = intLocation
    val tierNumber: Int get() = intTierNumber
    val displayItem: NeuInternalName get() = intDisplayItem

    private fun setTierNumber(tierNumber: Int) { this.intTierNumber = tierNumber }
    private fun setLocation(location: LorenzVec?) { this.intLocation = location }
    private fun setDisplayItem(displayItem: NeuInternalName) { this.intDisplayItem = displayItem }

    fun getTieredDisplayName() = "Tier $intTierNumber ($displayName)"

    @SkyHanniModule
    companion object {
        private val patternGroup = RepoPattern.group("crimson.kuudra")

        /**
         * REGEX-TEST: Kill Kuudra Basic Tier
         * REGEX-TEST: Kill Kuudra Fiery Tier
         */
        private val kuudraQuestPattern by patternGroup.pattern(
            "quest.identifier",
            "Kill Kuudra (?<tier>\\w+) Tier"
        )

        fun getQuestOrNull(
            questName: String,
            state: QuestState,
        ): KuudraQuest? = kuudraQuestPattern.matchMatcher(questName) {
            val tierName = getTierByNameOrNull(group("tier")) ?: return@matchMatcher null
            KuudraQuest(tierName, state)
        }

        private fun getTierByNameOrNull(name: String) = entries.firstOrNull {
            it.displayName.lowercase() == name.lowercase() || it.name.lowercase() == name.lowercase()
        }

        fun addRepoData(
            displayName: String,
            displayItem: NeuInternalName,
            location: LorenzVec?,
            tier: Int,
        ) {
            val target = entries.firstOrNull { it.displayName == displayName } ?: return
            target.setLocation(location)
            target.setDisplayItem(displayItem)
            target.setTierNumber(tier)
        }
    }
}
