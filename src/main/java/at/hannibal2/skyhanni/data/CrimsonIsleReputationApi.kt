package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.event.HandleEvent.Companion.LOWEST
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.NeuProfileDataLoadedEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.features.nether.reputationhelper.CrimsonIsleReputationHelper.factionType
import at.hannibal2.skyhanni.features.nether.reputationhelper.FactionType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlinx.serialization.json.JsonObject

@SkyHanniModule
object CrimsonIsleReputationApi {

    private val patternGroup = RepoPattern.group("crimson.reputationapi")

    /**
     * REGEX-TEST:  19,130
     * REGEX-TEST:  635
     */
    val tablistRepRegex by patternGroup.pattern(
        "tablistreputation",
        " (?<rep>(?:\\d+,?)+)",
    )

    // LOWEST to avoid running before the factionType updating in ReputationHelper Updater.
    @HandleEvent(priority = LOWEST)
    fun onWidgetUpdateEvent(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.REPUTATION)) return
        val currentFaction = factionType ?: return
        val factionReputation = ProfileStorageData.profileSpecific?.crimsonIsleReputation ?: return

        tablistRepRegex.firstMatcher(event.widget.lines.map { it.string }) {
            val currentRep = group("rep").replace(",", "").toInt()
            ChatUtils.debug("Tried Setting ${currentFaction.factionName} Reputation to $currentRep")
            factionReputation[currentFaction] = currentRep
        }
    }

    @HandleEvent
    fun onProfileViewerLoad(event: NeuProfileDataLoadedEvent) {
        val factionReputation = ProfileStorageData.profileSpecific?.crimsonIsleReputation ?: return
        val facInfo = event.getCurrentPlayerData()?.netherData ?: return
        factionReputation[FactionType.MAGE] = facInfo.mageReputation
        ChatUtils.debug("Set Mage Reputation to ${facInfo.mageReputation}")
        factionReputation[FactionType.BARBARIAN] = facInfo.barbarianReputation
        ChatUtils.debug("Set Barbarian Reputation to ${facInfo.barbarianReputation}")
    }
}
