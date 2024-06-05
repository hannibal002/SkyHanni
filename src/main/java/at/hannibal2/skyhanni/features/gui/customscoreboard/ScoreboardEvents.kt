package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.Companion.eventsConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardEvents.VOTING
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.features.misc.ServerRestartTitle
import at.hannibal2.skyhanni.features.rift.area.stillgorechateau.RiftBloodEffigies
import at.hannibal2.skyhanni.utils.CollectionUtils.addIfNotNull
import at.hannibal2.skyhanni.utils.CollectionUtils.nextAfter
import at.hannibal2.skyhanni.utils.CollectionUtils.sublistAfter
import at.hannibal2.skyhanni.utils.LorenzUtils.inAdvancedMiningIsland
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.RegexUtils.allMatches
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatches
import at.hannibal2.skyhanni.utils.RegexUtils.matchFirst
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.TabListData
import java.util.function.Supplier
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern as SbPattern

/**
 * This enum contains all the lines that either are events or other lines that are so rare/not often seen that they
 * don't fit in the normal [ScoreboardElement] enum.
 *
 * We for example have the [VOTING] Event, while this is clearly not an event, I don't consider them as normal lines
 * because they are visible for a maximum of like 1 minute every 5 days and ~12 hours.
 */

private fun getSbLines(): List<String> {
    return ScoreboardData.sidebarLinesFormatted
}

enum class ScoreboardEvents(
    private val displayLine: Supplier<List<String>>,
    private val showWhen: () -> Boolean,
    private val configLine: String,
) {
    VOTING(
        ::getVotingLines,
        ::getVotingShowWhen,
        "§7(All Voting Lines)"
    ),
    SERVER_CLOSE(
        ::getServerCloseLines,
        ::getServerCloseShowWhen,
        "§cServer closing soon!"
    ),
    DUNGEONS(
        ::getDungeonsLines,
        ::getDungeonsShowWhen,
        "§7(All Dungeons Lines)"
    ),
    KUUDRA(
        ::getKuudraLines,
        ::getKuudraShowWhen,
        "§7(All Kuudra Lines)"
    ),
    DOJO(
        ::getDojoLines,
        ::getDojoShowWhen,
        "§7(All Dojo Lines)"
    ),
    DARK_AUCTION(
        ::getDarkAuctionLines,
        ::getDarkAuctionShowWhen,
        "Time Left: §b11\n" +
            "Current Item:\n" +
            " §5Travel Scroll to Sirius"
    ),
    JACOB_CONTEST(
        ::getJacobContestLines,
        ::getJacobContestShowWhen,
        "§eJacob's Contest\n" +
            "§e○ §fCarrot §a18m17s\n" +
            " Collected §e8,264"
    ),
    JACOB_MEDALS(
        ::getJacobMedalsLines,
        ::getJacobMedalsShowWhen,
        "§6§lGOLD §fmedals: §613\n" +
            "§f§lSILVER §fmedals: §f3\n" +
            "§c§lBRONZE §fmedals: §c4"
    ),
    TRAPPER(
        ::getTrapperLines,
        ::getTrapperShowWhen,
        "Pelts: §5711\n" +
            "Tracker Mob Location:\n" +
            "§bMushroom Gorge"
    ),
    GARDEN_CLEAN_UP(
        ::getGardenCleanUpLines,
        ::getGardenCleanUpShowWhen,
        "Cleanup: §c12.6%"
    ),
    GARDEN_PASTING(
        ::getGardenPastingLines,
        ::getGardenPastingShowWhen,
        "§fBarn Pasting§7: §e12.3%"
    ),
    FLIGHT_DURATION(
        ::getFlightDurationLines,
        ::getFlightDurationShowWhen,
        "Flight Duration: §a10m 0s"
    ),
    WINTER(
        ::getWinterLines,
        ::getWinterShowWhen,
        "§7(All Winter Event Lines)"
    ),
    SPOOKY(
        ::getSpookyLines,
        ::getSpookyShowWhen,
        "§6Spooky Festival§f 50:54\n" +
            "§7Your Candy:\n" +
            "§a1 Green§7, §50 Purple §7(§61 §7pts.)"
    ),
    BROODMOTHER(
        ::getBroodmotherLines,
        ::getBroodmotherShowWhen,
        "§4Broodmother§7: §eDormant"
    ),
    MINING_EVENTS(
        ::getMiningEventsLines,
        { inAdvancedMiningIsland() },
        "§7(All Mining Event Lines)"
    ),
    DAMAGE(
        ::getDamageLines,
        ::getDamageShowWhen,
        "Dragon HP: §a6,180,925 §c❤\n" +
            "Your Damage: §c375,298.5"
    ),
    MAGMA_BOSS(
        ::getMagmaBossLines,
        ::getMagmaBossShowWhen,
        "§7(All Magma Boss Lines)\n" +
            "§7Boss: §c0%\n" +
            "§7Damage Soaked:\n" +
            "§e▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎§7▎▎▎▎▎"
    ),
    RIFT(
        ::getRiftLines,
        ::getRiftShowWhen,
        "§7(All Rift Lines)"
    ),
    ESSENCE(
        ::getEssenceLines,
        ::getEssenceShowWhen,
        "Dragon Essence: §d1,285"
    ),
    QUEUE(
        ::getQueueLines,
        ::getQueueShowWhen,
        "Queued: Glacite Mineshafts\n" +
            "Position: §b#45 §fSince: §a00:00"
    ),
    ACTIVE_TABLIST_EVENTS(
        ::getActiveEventLine,
        ::getActiveEventShowWhen,
        "§7(All Active Tablist Events)\n§dHoppity's Hunt\n §fEnds in: §e26h"
    ),
    STARTING_SOON_TABLIST_EVENTS(
        ::getSoonEventLine,
        ::getSoonEventShowWhen,
        "§7(All Starting Soon Tablist Events)\n§6Mining Fiesta\n §fStarts in: §e52min"
    ),
    REDSTONE(
        ::getRedstoneLines,
        ::getRedstoneShowWhen,
        "§e§l⚡ §cRedstone: §e§b7%"
    ),
    ;

    override fun toString() = configLine

    fun getLines(): List<String> = displayLine.get()

    companion object {
        fun getEvent() = buildList<ScoreboardEvents?> {
            if (eventsConfig.showAllActiveEvents) {
                for (event in eventsConfig.eventEntries) {
                    if (event.showWhen()) {
                        add(event)
                    }
                }
            } else {
                add(eventsConfig.eventEntries.firstOrNull { it.showWhen() && it.getLines().isNotEmpty() })
            }
        }

        // I don't know why, but this field is needed for it to work
        @JvmField
        val defaultOption = listOf(
            VOTING,
            SERVER_CLOSE,
            DUNGEONS,
            KUUDRA,
            DOJO,
            DARK_AUCTION,
            JACOB_CONTEST,
            JACOB_MEDALS,
            TRAPPER,
            GARDEN_CLEAN_UP,
            GARDEN_PASTING,
            FLIGHT_DURATION,
            WINTER,
            SPOOKY,
            BROODMOTHER,
            MINING_EVENTS,
            DAMAGE,
            MAGMA_BOSS,
            RIFT,
            ESSENCE,
            ACTIVE_TABLIST_EVENTS
        )
    }
}

private fun getVotingLines() = buildList {
    val sbLines = getSbLines()

    val yearLine = SbPattern.yearVotesPattern.firstMatches(sbLines) ?: return emptyList<String>()
    add(yearLine)

    if (sbLines.nextAfter(yearLine) == "§7Waiting for") {
        add("§7Waiting for")
        add("§7your vote...")
    } else {
        addAll(SbPattern.votesPattern.allMatches(sbLines))
    }
}

private fun getVotingShowWhen(): Boolean =
    SbPattern.yearVotesPattern.anyMatches(getSbLines()) // is empty on top already

private fun getServerCloseLines() = buildList {
    ServerRestartTitle.restartingGreedyPattern.firstMatches(getSbLines())?.let {
        add(it.split("§8")[0])
    }
}

private fun getServerCloseShowWhen(): Boolean =
    ServerRestartTitle.restartingGreedyPattern.anyMatches(getSbLines()) // is empty on top already

private fun getDungeonsLines() = listOf(
    SbPattern.m7dragonsPattern,
    SbPattern.autoClosingPattern,
    SbPattern.startingInPattern,
    SbPattern.keysPattern,
    SbPattern.timeElapsedPattern,
    SbPattern.clearedPattern,
    SbPattern.soloPattern,
    SbPattern.teammatesPattern,
    SbPattern.floor3GuardiansPattern
).allMatches(getSbLines()).map { it.removePrefix("§r") }

private fun getDungeonsShowWhen(): Boolean = DungeonAPI.inDungeon()

private fun getKuudraLines() = listOf(
    SbPattern.autoClosingPattern,
    SbPattern.startingInPattern,
    SbPattern.timeElapsedPattern,
    SbPattern.instanceShutdownPattern,
    SbPattern.wavePattern,
    SbPattern.tokensPattern,
    SbPattern.submergesPattern
).allMatches(getSbLines())

private fun getKuudraShowWhen(): Boolean = IslandType.KUUDRA_ARENA.isInIsland()

private fun getDojoLines() = listOf(
    SbPattern.dojoChallengePattern,
    SbPattern.dojoDifficultyPattern,
    SbPattern.dojoPointsPattern,
    SbPattern.dojoTimePattern
).allMatches(getSbLines())

private fun getDojoShowWhen(): Boolean =
    SbPattern.dojoChallengePattern.anyMatches(getSbLines()) // is empty on top already

private fun getDarkAuctionLines() = buildList {
    addAll(listOf(SbPattern.startingInPattern, SbPattern.timeLeftPattern).allMatches(getSbLines()))

    SbPattern.darkAuctionCurrentItemPattern.firstMatches(getSbLines())?.let {
        add(it)
        addIfNotNull(getSbLines().nextAfter(it))
    }
}

private fun getDarkAuctionShowWhen(): Boolean = IslandType.DARK_AUCTION.isInIsland()

private fun getJacobContestLines() = buildList {
    SbPattern.jacobsContestPattern.firstMatches(getSbLines())?.let { line ->
        add(line)
        addAll(getSbLines().sublistAfter(line, 3).filter {
            !SbPattern.footerPattern.matches(it)
        })
    }
}

private fun getJacobContestShowWhen(): Boolean =
    SbPattern.jacobsContestPattern.anyMatches(getSbLines())  // is empty on top already

private fun getJacobMedalsLines(): List<String> = SbPattern.medalsPattern.allMatches(getSbLines())

private fun getJacobMedalsShowWhen(): Boolean =
    SbPattern.medalsPattern.anyMatches(getSbLines()) // is empty on top already

private fun getTrapperLines() = buildList {
    addIfNotNull(SbPattern.peltsPattern.firstMatches(getSbLines()))
    SbPattern.mobLocationPattern.firstMatches(getSbLines())?.let {
        add("Tracker Mob Location:")
        addIfNotNull(getSbLines().nextAfter(it))
    }
}

private fun getTrapperShowWhen(): Boolean =
    listOf(
        ScoreboardPattern.peltsPattern,
        ScoreboardPattern.mobLocationPattern
    ).anyMatches(getSbLines())  // is empty on top already

private fun getGardenCleanUpLines() = buildList {
    addIfNotNull(SbPattern.cleanUpPattern.firstMatches(getSbLines())?.trim())
}

private fun getGardenCleanUpShowWhen(): Boolean =
    SbPattern.cleanUpPattern.anyMatches(getSbLines()) // is empty on top already

private fun getGardenPastingLines() = buildList {
    addIfNotNull(SbPattern.pastingPattern.firstMatches(getSbLines())?.trim())
}

private fun getGardenPastingShowWhen(): Boolean =
    SbPattern.pastingPattern.anyMatches(getSbLines()) // is empty on top already

// Doesn't exist anymore
private fun getFlightDurationLines() = buildList {
    addIfNotNull(SbPattern.flightDurationPattern.firstMatches(getSbLines())?.trim())
}

private fun getFlightDurationShowWhen(): Boolean =
    SbPattern.flightDurationPattern.anyMatches(getSbLines()) // is empty on top already

private fun getWinterLines() = buildList {
    addAll(listOf(
        SbPattern.winterEventStartPattern,
        SbPattern.winterNextWavePattern,
        SbPattern.winterWavePattern,
        SbPattern.winterMagmaLeftPattern,
        SbPattern.winterTotalDmgPattern,
        SbPattern.winterCubeDmgPattern
    ).allMatches(getSbLines()).filter { it.endsWith("Soon!") })
}

private fun getWinterShowWhen(): Boolean = // is empty on top already
    listOf(
        SbPattern.winterEventStartPattern,
        SbPattern.winterNextWavePattern,
        SbPattern.winterWavePattern
    ).anyMatches(getSbLines())

private fun getSpookyLines() = buildList {
    SbPattern.spookyPattern.firstMatches(getSbLines())?.let { // Time
        add(it)
        add("§7Your Candy: ")
        add(
            TabListData.getFooter()
                .removeResets()
                .split("\n")
                .firstOrNull { it.startsWith("§7Your Candy:") }
                ?.removePrefix("§7Your Candy:") ?: "§cCandy not found"
        ) // Candy
    }
}

private fun getSpookyShowWhen(): Boolean = SbPattern.spookyPattern.anyMatches(getSbLines()) // is empty on top already

private fun getTablistEvent(): String? {
    TabListData.getTabList().matchFirst(SbPattern.eventNamePattern) {
        return group("name")
    } ?: return null
}

private fun getActiveEventLine(): List<String> {
    val currentActiveEvent = getTablistEvent() ?: return emptyList()

    // Some Active Events are better not shown from the tablist,
    // but from other locations like the scoreboard
    val blockedEvents = listOf("Spooky Festival")
    if (blockedEvents.contains(currentActiveEvent.removeColor())) return emptyList()
    val currentActiveEventTime = TabListData.getTabList().matchFirst(SbPattern.eventTimeEndsPattern) {
        group("time")
    } ?: "§cUnknown"

    return listOf(currentActiveEvent, " Ends in: §e$currentActiveEventTime")
}

private fun getActiveEventShowWhen(): Boolean =
    getTablistEvent() != null && SbPattern.eventTimeEndsPattern.anyMatches(TabListData.getTabList()) // is empty on top already

private fun getSoonEventLine(): List<String> {
    val soonActiveEvent = getTablistEvent() ?: return emptyList()
    val soonActiveEventTime = TabListData.getTabList().matchFirst(SbPattern.eventTimeStartsPattern) {
        group("time")
    } ?: "§cUnknown"

    return listOf(soonActiveEvent, " Starts in: §e$soonActiveEventTime")
}

private fun getSoonEventShowWhen(): Boolean =
    getTablistEvent() != null && TabListData.getTabList()
        .any { SbPattern.eventTimeStartsPattern.matches(it) } // is empty on top already

private fun getBroodmotherLines() = buildList {
    addIfNotNull(SbPattern.broodmotherPattern.firstMatches(getSbLines()))
}

private fun getBroodmotherShowWhen(): Boolean =
    SbPattern.broodmotherPattern.anyMatches(getSbLines()) // is empty on top already

private fun getMiningEventsLines() = buildList {
    // Wind
    val (compassTitle, compassArrow) = SbPattern.windCompassPattern.firstMatches(getSbLines()) to
        SbPattern.windCompassArrowPattern.firstMatches(getSbLines())
    if (compassTitle != null && compassArrow != null) {
        add(compassTitle)
        add("| $compassArrow §f|")
    }

    // Better Together
    SbPattern.nearbyPlayersPattern.firstMatches(getSbLines())?.let {
        add("§dBetter Together")
        add(" $it")
    }

    // Zone Events
    val zoneEvent = SbPattern.miningEventPattern.firstMatches(getSbLines())
    zoneEvent?.let { eventTitle ->
        add(eventTitle.removePrefix("Event: "))
        SbPattern.miningEventZonePattern.firstMatches(getSbLines())?.let { zone ->
            add("in ${zone.removePrefix("Zone: ")}")
        }
    }

    // Mithril Gourmand
    addAll(
        listOf(
            SbPattern.mithrilRemainingPattern,
            SbPattern.mithrilYourMithrilPattern
        ).allMatches(getSbLines())
    )

    // Raffle
    addAll(
        listOf(
            SbPattern.raffleTicketsPattern,
            SbPattern.rafflePoolPattern
        ).allMatches(getSbLines())
    )

    // Raid
    addAll(
        listOf(
            SbPattern.yourGoblinKillsPattern,
            SbPattern.remainingGoblinPattern,
        ).allMatches(getSbLines())
    )

    // Fortunate Freezing
    addIfNotNull(SbPattern.fortunateFreezingBonusPattern.firstMatches(getSbLines()))

    // Fossil Dust
    addIfNotNull(SbPattern.fossilDustPattern.firstMatches(getSbLines()))
}

private fun getDamageLines(): List<String> =
    listOf(
        listOf(
            SbPattern.bossHPPattern,
            SbPattern.bossDamagePattern
        ).allMatches(getSbLines()).joinToString("")
    )

private fun getDamageShowWhen(): Boolean =
    listOf(
        SbPattern.bossHPPattern,
        SbPattern.bossDamagePattern
    ).anyMatches(getSbLines()) // is empty on top already

private fun getMagmaBossLines() = listOf(
    SbPattern.magmaBossPattern,
    SbPattern.damageSoakedPattern,
    SbPattern.killMagmasPattern,
    SbPattern.killMagmasDamagedSoakedBarPattern,
    SbPattern.reformingPattern,
    SbPattern.bossHealthPattern,
    SbPattern.bossHealthBarPattern
).allMatches(getSbLines())

private fun getMagmaBossShowWhen(): Boolean = SbPattern.magmaChamberPattern.matches(HypixelData.skyBlockArea)

private fun getRiftLines() = listOf(
    RiftBloodEffigies.heartsPattern,
    SbPattern.riftHotdogTitlePattern,
    SbPattern.timeLeftPattern,
    SbPattern.riftHotdogEatenPattern,
    SbPattern.riftAveikxPattern,
    SbPattern.riftHayEatenPattern,
    SbPattern.cluesPattern
).allMatches(getSbLines())

private fun getRiftShowWhen(): Boolean = IslandType.THE_RIFT.isInIsland()

private fun getEssenceLines() = buildList {
    addIfNotNull(SbPattern.essencePattern.firstMatches(getSbLines()))
}

private fun getEssenceShowWhen(): Boolean = SbPattern.essencePattern.anyMatches(getSbLines()) // is empty on top already

private fun getQueueLines(): List<String> = listOf(
    SbPattern.queuePattern,
    SbPattern.queuePositionPattern
).allMatches(getSbLines())

private fun getQueueShowWhen(): Boolean = SbPattern.queuePattern.anyMatches(getSbLines()) // is empty on top already

private fun getRedstoneLines() = buildList {
    addIfNotNull(SbPattern.redstonePattern.firstMatches(getSbLines()))
}

private fun getRedstoneShowWhen(): Boolean = SbPattern.redstonePattern.anyMatches(getSbLines())
