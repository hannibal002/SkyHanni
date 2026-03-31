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

    private val storage get() = ProfileStorageData.profileSpecific
    private val crimsonStorage get() = storage?.crimsonIsle

    var factionType
        get() = storage?.crimsonIsleFaction
        set(it) {
            storage?.crimsonIsleFaction = it
        }

    @HandleEvent
    fun onWidgetUpdateEvent(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.REPUTATION)) return

        TabWidget.REPUTATION.matchMatcherFirstLine {
            factionType = FactionType.fromName(group("faction"))
        }
        val currentFaction = factionType ?: return

        tablistReputationCountPattern.firstMatcher(event.widget.lines.map { it.string }) {
            val currentRep = group("rep").replace(",", "").toInt()
            ChatUtils.debug("Tried Setting ${currentFaction.factionName} Reputation to $currentRep")
            crimsonStorage?.reputation[currentFaction] = currentRep
        }
    }

    @HandleEvent
    fun onProfileViewerLoad(event: ProfileViewerDataLoadedEvent) {
        val faction = event.getCurrentPlayerData()?.netherData ?: return
        factionType = FactionType.fromAPIName(faction.currentFaction)
        crimsonStorage?.reputation[FactionType.MAGE] = faction.mageReputation
        ChatUtils.debug("Set Mage Reputation to ${faction.mageReputation}")
        crimsonStorage?.reputation[FactionType.BARBARIAN] = faction.barbarianReputation
        ChatUtils.debug("Set Barbarian Reputation to ${faction.barbarianReputation}")
    }
}
