package at.hannibal2.skyhanni.features.event.jerry.frozentreasure

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.WinterApi
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.annotations.Expose

@SkyHanniModule
object FrozenTreasureTracker {

    private val config get() = SkyHanniMod.feature.event.winter.frozenTreasureTracker

    private val compactPattern by RepoPattern.pattern(
        "event.jerry.frozentreasure.compact",
        "COMPACT! You found an Enchanted Ice!",
    )

    private var estimatedIce = 0L
    private var lastEstimatedIce = 0L
    private var icePerSecond = mutableListOf<Long>()
    private var icePerHour = 0
    private var stoppedChecks = 0
    private val tracker = SkyHanniTracker(
        "Frozen Treasure Tracker",
        ::Data,
        { it.frozenTreasureTracker },
        trackerConfig = { config.perTrackerConfig }
    ) {
        formatDisplay(drawDisplay(it))
    }

    init {
        FrozenTreasure.entries.forEach { it.chatPattern }
    }

    data class Data(
        @Expose var treasuresMined: Long = 0,
        @Expose var compactProcs: Long = 0,
        @Expose var treasureCount: MutableMap<FrozenTreasure, Int> = mutableMapOf(),
    ) : TrackerData()

    @HandleEvent
    fun onWorldChange() {
        icePerHour = 0
        stoppedChecks = 0
        icePerSecond = mutableListOf()
        tracker.update()
    }

    @HandleEvent(onlyOnIsland = IslandType.WINTER)
    fun onSecondPassed() {
        val difference = estimatedIce - lastEstimatedIce
        lastEstimatedIce = estimatedIce

        if (difference == estimatedIce) return

        if (difference == 0L) {
            if (icePerSecond.isEmpty()) return
            stoppedChecks += 1
        } else {
            if (stoppedChecks > 60) {
                stoppedChecks = 0
                icePerSecond.clear()
                icePerHour = 0
            }
            while (stoppedChecks > 0) {
                stoppedChecks -= 1
                icePerSecond.add(0)
            }
            icePerSecond.add(difference)
            val listCopy = icePerSecond
            while (listCopy.size > 1200) listCopy.removeAt(0)
            icePerSecond = listCopy
        }
        icePerHour = (icePerSecond.average() * 3600).toInt()
    }

    private fun formatDisplay(map: List<Searchable>): List<Searchable> {
        val newList = mutableListOf<Searchable>()
        for (index in config.textFormat) {
            // TODO, change functionality to use enum rather than ordinals
            newList.add(map[index.ordinal])
        }
        return newList
    }

    @HandleEvent(onlyOnIsland = IslandType.WINTER)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!ProfileStorageData.loaded) return

        val message = event.cleanMessage.trim()

        compactPattern.matchMatcher(message) {
            tracker.modify {
                it.compactProcs += 1
            }
            if (config.hideMessages) event.blockedReason = "frozen treasure tracker"
        }

        for (treasure in FrozenTreasure.entries.filter { it.chatPattern.matches(message) }) {
            tracker.modify {
                it.treasuresMined += 1
                it.treasureCount.addOrPut(treasure, 1)
            }
            if (config.hideMessages) event.blockedReason = "frozen treasure tracker"
        }
    }

    private fun drawDisplay(data: Data) = buildList {
        calculateIce(data)
        addSearchString("§e§lFrozen Treasure Tracker")
        addSearchString("§6${formatNumber(data.treasuresMined)} Treasures Mined")
        addSearchString("§3${formatNumber(estimatedIce)} Total Ice")
        addSearchString("§3${formatNumber(icePerHour)} Ice/hr")
        addSearchString("§8${formatNumber(data.compactProcs)} Compact Procs")
        addSearchString("")

        for (treasure in FrozenTreasure.entries) {
            val count = (data.treasureCount[treasure] ?: 0) * if (config.showAsDrops) treasure.defaultAmount else 1
            addSearchString("§b${formatNumber(count)} ${treasure.displayName}", treasure.displayName)
        }
        addSearchString("")
    }

    private fun formatNumber(amount: Number): String {
        if (amount is Int) return amount.addSeparators()
        if (amount is Long) return amount.shortFormat()
        return "$amount"
    }

    private fun calculateIce(data: Data) {
        estimatedIce = data.compactProcs * 160L
        for (treasure in FrozenTreasure.entries) {
            val amount = data.treasureCount[treasure] ?: 0
            estimatedIce += amount * treasure.defaultAmount * treasure.iceMultiplier
        }
    }

    init {
        tracker.initRenderer({ config.position }) { shouldShowDisplay() }
    }

    private fun shouldShowDisplay(): Boolean {
        if (!config.enabled) return false
        if (!WinterApi.inWorkshop()) return false
        if (config.onlyInCave && !WinterApi.inGlacialCave()) return false

        return true
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.frozenTreasureTracker", "event.winter.frozenTreasureTracker")
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetfrozentreasuretracker") {
            description = "Resets the Frozen Treasure Tracker"
            category = CommandCategory.USERS_RESET
            simpleCallback { tracker.resetCommand() }
        }
    }
}
