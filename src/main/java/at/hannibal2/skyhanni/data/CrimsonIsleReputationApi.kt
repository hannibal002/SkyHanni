package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.ProfileViewerDataLoadedEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.features.nether.reputationhelper.FactionType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object CrimsonIsleReputationApi {

    private val patternGroup = RepoPattern.group("crimson.reputationapi")

    /**
     * REGEX-TEST:  19,130
     * REGEX-TEST:  635
     */
    private val tablistReputationCountPattern by patternGroup.pattern(
        "tablistreputation",
        " (?<rep>(?:\\d+,?)+)",
    )

    var factionType get() = ProfileStorageData.profileSpecific?.crimsonIsleFaction
        set(it) {
            ProfileStorageData.profileSpecific?.crimsonIsleFaction = it
        }

    // LOWEST to avoid running before the factionType updating in ReputationHelper Updater.
    @HandleEvent()
    fun onWidgetUpdateEvent(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.REPUTATION)) return

        TabWidget.REPUTATION.matchMatcherFirstLine {
            factionType = FactionType.fromName(group("faction"))
        }
        val currentFaction = factionType ?: return

        tablistReputationCountPattern.firstMatcher(event.widget.lines.map { it.string }) {
            val currentRep = group("rep").replace(",", "").toInt()
            ChatUtils.debug("Tried Setting ${currentFaction.factionName} Reputation to $currentRep")
            ProfileStorageData.profileSpecific?.crimsonIsle?.reputation[currentFaction] = currentRep
        }
    }

    @HandleEvent
    fun onProfileViewerLoad(event: ProfileViewerDataLoadedEvent) {
        val facInfo = event.getCurrentPlayerData()?.netherData ?: return
        val faction = event.getCurrentPlayerData()?.netherData?.currentFaction.orEmpty()
        factionType = FactionType.fromAPIName(faction)
        ProfileStorageData.profileSpecific?.crimsonIsle?.reputation[FactionType.MAGE] = facInfo.mageReputation
        ChatUtils.debug("Set Mage Reputation to ${facInfo.mageReputation}")
        ProfileStorageData.profileSpecific?.crimsonIsle?.reputation[FactionType.BARBARIAN] = facInfo.barbarianReputation
        ChatUtils.debug("Set Barbarian Reputation to ${facInfo.barbarianReputation}")
    }
}
