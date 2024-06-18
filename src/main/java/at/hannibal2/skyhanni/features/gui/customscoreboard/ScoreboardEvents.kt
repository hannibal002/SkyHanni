package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.data.FlightDurationAPI
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.data.WinterAPI
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.eventsConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getElementFromAny
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardEvents.VOTING
import at.hannibal2.skyhanni.features.misc.ServerRestartTitle
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraAPI
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.features.rift.area.stillgorechateau.RiftBloodEffigies
import at.hannibal2.skyhanni.utils.CollectionUtils.addNotNull
import at.hannibal2.skyhanni.utils.CollectionUtils.nextAfter
import at.hannibal2.skyhanni.utils.CollectionUtils.sublistAfter
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.inAdvancedMiningIsland
import at.hannibal2.skyhanni.utils.LorenzUtils.inAnyIsland
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.RegexUtils.allMatches
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatches
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.TimeUtils.format
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
    private val displayLine: Supplier<List<Any>>,
    private val showWhen: () -> Boolean,
    private val configLine: String,
) {
    VOTING(
        ::getVotingLines,
        { IslandType.HUB.isInIsland() },
        "§7(All Voting Lines)",
    ),
    SERVER_CLOSE(
        ::getServerCloseLines,
        { true },
        "§cServer closing soon!",
    ),
    DUNGEONS(
        ::getDungeonsLines,
        { DungeonAPI.inDungeon() },
        "§7(All Dungeons Lines)",
    ),
    KUUDRA(
        ::getKuudraLines,
        { KuudraAPI.inKuudra() },
        "§7(All Kuudra Lines)",
    ),
    DOJO(
        ::getDojoLines,
        ::getDojoShowWhen,
        "§7(All Dojo Lines)",
    ),
    DARK_AUCTION(
        ::getDarkAuctionLines,
        { IslandType.DARK_AUCTION.isInIsland() },
        "Time Left: §b11\n" +
            "Current Item:\n" +
            " §5Travel Scroll to Sirius",
    ),
    JACOB_CONTEST(
        ::getJacobContestLines,
        { true },
        "§eJacob's Contest\n" +
            "§e○ §fCarrot §a18m17s\n" +
            " Collected §e8,264",
    ),
    JACOB_MEDALS(
        ::getJacobMedalsLines,
        ::getJacobMedalsShowWhen,
        "§6§lGOLD §fmedals: §613\n" +
            "§f§lSILVER §fmedals: §f3\n" +
            "§c§lBRONZE §fmedals: §c4",
    ),
    TRAPPER(
        ::getTrapperLines,
        { inAnyIsland(IslandType.THE_FARMING_ISLANDS) },
        "Pelts: §5711\n" +
            "Tracker Mob Location:\n" +
            "§bMushroom Gorge",
    ),
    GARDEN_CLEAN_UP(
        ::getGardenCleanUpLines,
        { GardenAPI.inGarden() },
        "Cleanup: §c12.6%",
    ),
    GARDEN_PASTING(
        ::getGardenPastingLines,
        { GardenAPI.inGarden() },
        "§fBarn Pasting§7: §e12.3%",
    ),
    FLIGHT_DURATION(
        ::getFlightDurationLines,
        ::getFlightDurationShowWhen,
        "Flight Duration: §a10m 0s",
    ),
    WINTER(
        ::getWinterLines,
        { WinterAPI.inWorkshop() },
        "§7(All Winter Event Lines)",
    ),
    NEW_YEAR(
        ::getNewYearLines,
        ::getNewYearShowWhen,
        "§dNew Year Event!§f 24:25",
    ),
    SPOOKY(
        ::getSpookyLines,
        { true },
        "§6Spooky Festival§f 50:54\n" +
            "§7Your Candy:\n" +
            "§a1 Green§7, §50 Purple §7(§61 §7pts.)",
    ),
    BROODMOTHER(
        ::getBroodmotherLines,
        { IslandType.SPIDER_DEN.isInIsland() },
        "§4Broodmother§7: §eDormant",
    ),
    MINING_EVENTS(
        ::getMiningEventsLines,
        { inAdvancedMiningIsland() },
        "§7(All Mining Event Lines)",
    ),
    DAMAGE(
        ::getDamageLines,
        { true },
        "Dragon HP: §a6,180,925 §c❤\n" +
            "Your Damage: §c375,298.5",
    ),
    MAGMA_BOSS(
        ::getMagmaBossLines,
        ::getMagmaBossShowWhen,
        "§7(All Magma Boss Lines)\n" +
            "§7Boss: §c0%\n" +
            "§7Damage Soaked:\n" +
            "§e▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎§7▎▎▎▎▎",
    ),
    CARNIVAL(
        ::getCarnivalLines,
        ::getCarnivalShowWhen,
        "§7(All Carnival Lines)",
    ),
    RIFT(
        ::getRiftLines,
        { RiftAPI.inRift() },
        "§7(All Rift Lines)",
    ),
    ESSENCE(
        ::getEssenceLines,
        { true },
        "Dragon Essence: §d1,285",
    ),
    QUEUE(
        ::getQueueLines,
        { true },
        "Queued: Glacite Mineshafts\n" +
            "Position: §b#45 §fSince: §a00:00",
    ),
    ACTIVE_TABLIST_EVENTS(
        ::getActiveEventLine,
        ::getActiveEventShowWhen,
        "§7(All Active Tablist Events)\n§dHoppity's Hunt\n §fEnds in: §e26h",
    ),
    STARTING_SOON_TABLIST_EVENTS(
        ::getSoonEventLine,
        { true },
        "§7(All Starting Soon Tablist Events)\n§6Mining Fiesta\n §fStarts in: §e52min",
    ),
    REDSTONE(
        ::getRedstoneLines,
        { IslandType.PRIVATE_ISLAND.isInIsland() },
        "§e§l⚡ §cRedstone: §e§b7%",
    ),
    ANNIVERSARY(
        ::getAnniversaryLines,
        ::getAnniversaryShowWhen,
        "§d5th Anniversary§f 167:59:54",
    ),
    ;

    override fun toString() = configLine

    fun getLines(): List<ScoreboardElementType> = displayLine.get().map { getElementFromAny(it) }

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
            NEW_YEAR,
            WINTER,
            SPOOKY,
            BROODMOTHER,
            MINING_EVENTS,
            DAMAGE,
            MAGMA_BOSS,
            CARNIVAL,
            RIFT,
            ESSENCE,
            ACTIVE_TABLIST_EVENTS,
        )
    }
}

private fun getVotingLines() = buildList {
    val sbLines = getSbLines()

    val yearLine = SbPattern.yearVotesPattern.firstMatches(sbLines) ?: return listOf<String>()
    add(yearLine)

    if (sbLines.nextAfter(yearLine) == "§7Waiting for") {
        add("§7Waiting for")
        add("§7your vote...")
    } else {
        addAll(SbPattern.votesPattern.allMatches(sbLines))
    }
}

private fun getServerCloseLines() = buildList {
    ServerRestartTitle.restartingGreedyPattern.firstMatches(getSbLines())?.let {
        add(it.split("§8")[0])
    }
}

private fun getDungeonsLines() = listOf(
    SbPattern.m7dragonsPattern,
    SbPattern.autoClosingPattern,
    SbPattern.startingInPattern,
    SbPattern.keysPattern,
    SbPattern.timeElapsedPattern,
    SbPattern.clearedPattern,
    SbPattern.soloPattern,
    SbPattern.teammatesPattern,
    SbPattern.floor3GuardiansPattern,
).allMatches(getSbLines()).map { it.removePrefix("§r") }

private fun getKuudraLines() = listOf(
    SbPattern.autoClosingPattern,
    SbPattern.startingInPattern,
    SbPattern.timeElapsedPattern,
    SbPattern.instanceShutdownPattern,
    SbPattern.wavePattern,
    SbPattern.tokensPattern,
    SbPattern.submergesPattern,
).allMatches(getSbLines())

private fun getDojoLines() = listOf(
    SbPattern.dojoChallengePattern,
    SbPattern.dojoDifficultyPattern,
    SbPattern.dojoPointsPattern,
    SbPattern.dojoTimePattern,
).allMatches(getSbLines())

private fun getDojoShowWhen(): Boolean = IslandType.CRIMSON_ISLE.isInIsland() && LorenzUtils.skyBlockArea == "Dojo"

private fun getDarkAuctionLines() = buildList {
    addAll(listOf(SbPattern.startingInPattern, SbPattern.timeLeftPattern).allMatches(getSbLines()))

    SbPattern.darkAuctionCurrentItemPattern.firstMatches(getSbLines())?.let {
        add(it)
        addNotNull(getSbLines().nextAfter(it))
    }
}

private fun getJacobContestLines() = buildList {
    SbPattern.jacobsContestPattern.firstMatches(getSbLines())?.let { line ->
        add(line)
        addAll(
            getSbLines().sublistAfter(line, 3).filter {
                !SbPattern.footerPattern.matches(it)
            },
        )
    }
}

private fun getJacobMedalsLines(): List<String> = SbPattern.medalsPattern.allMatches(getSbLines())

private fun getJacobMedalsShowWhen(): Boolean = inAnyIsland(IslandType.GARDEN, IslandType.HUB)

private fun getTrapperLines() = buildList {
    addNotNull(SbPattern.peltsPattern.firstMatches(getSbLines()))
    SbPattern.mobLocationPattern.firstMatches(getSbLines())?.let {
        add("Tracker Mob Location:")
        addNotNull(getSbLines().nextAfter(it))
    }
}

private fun getGardenCleanUpLines() = listOfNotNull(SbPattern.cleanUpPattern.firstMatches(getSbLines())?.trim())

private fun getGardenPastingLines() = listOfNotNull(SbPattern.pastingPattern.firstMatches(getSbLines())?.trim())

private fun getFlightDurationLines() = listOf("Flight Duration: §a${FlightDurationAPI.flightDuration.format(maxUnits = 2)}")

private fun getFlightDurationShowWhen(): Boolean = FlightDurationAPI.isFlyingActive()

private fun getWinterLines() = listOf(
    SbPattern.winterEventStartPattern,
    SbPattern.winterNextWavePattern,
    SbPattern.winterWavePattern,
    SbPattern.winterMagmaLeftPattern,
    SbPattern.winterTotalDmgPattern,
    SbPattern.winterCubeDmgPattern,
).allMatches(getSbLines()).filter { !it.endsWith("Soon!") }

private fun getNewYearLines() = listOf(getSbLines().first { SbPattern.newYearPattern.matches(it) })

private fun getNewYearShowWhen(): Boolean = SbPattern.newYearPattern.anyMatches(getSbLines())

private fun getSpookyLines() = buildList {
    SbPattern.spookyPattern.firstMatches(getSbLines())?.let {
        // Time
        add(it)
        add("§7Your Candy: ")
        add(
            TabListData.getFooter()
                .removeResets()
                .split("\n")
                .firstOrNull { it.startsWith("§7Your Candy:") }
                ?.removePrefix("§7Your Candy:") ?: "§cCandy not found",
        ) // Candy
    }
}

private fun getTablistEvent(): String? {
    return SbPattern.eventNamePattern.firstMatcher(TabListData.getTabList()) {
        group("name")
    }
}

private fun getActiveEventLine(): List<String> {
    val currentActiveEvent = getTablistEvent() ?: return listOf()

    // Some Active Events are better not shown from the tablist,
    // but from other locations like the scoreboard
    val blockedEvents = listOf("Spooky Festival", "Carnival", "5th SkyBlock Anniversary", "New Year Celebration")
    if (blockedEvents.contains(currentActiveEvent.removeColor())) return listOf()
    val currentActiveEventTime = SbPattern.eventTimeEndsPattern.firstMatcher(TabListData.getTabList()) {
        group("time")
    } ?: "§cUnknown"

    return listOf(currentActiveEvent, " Ends in: §e$currentActiveEventTime")
}

private fun getActiveEventShowWhen(): Boolean = true

private fun getSoonEventLine(): List<String> {
    val soonActiveEvent = getTablistEvent() ?: return listOf()
    val soonActiveEventTime = SbPattern.eventTimeStartsPattern.firstMatcher(TabListData.getTabList()) {
        group("time")
    } ?: "§cUnknown"

    return listOf(soonActiveEvent, " Starts in: §e$soonActiveEventTime")
}

private fun getBroodmotherLines() = listOfNotNull(SbPattern.broodmotherPattern.firstMatches(getSbLines()))

private fun getMiningEventsLines() = buildList {
    // Wind
    val (compassTitle, compassArrow) = SbPattern.windCompassPattern.firstMatches(getSbLines()) to
        SbPattern.windCompassArrowPattern.firstMatches(getSbLines())
    if (compassTitle != null && compassArrow != null) {
        add(compassTitle)
        add(compassArrow to HorizontalAlignment.CENTER)
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
            SbPattern.mithrilYourMithrilPattern,
        ).allMatches(getSbLines()),
    )

    // Raffle
    addAll(
        listOf(
            SbPattern.raffleTicketsPattern,
            SbPattern.rafflePoolPattern,
        ).allMatches(getSbLines()),
    )

    // Raid
    addAll(
        listOf(
            SbPattern.yourGoblinKillsPattern,
            SbPattern.remainingGoblinPattern,
        ).allMatches(getSbLines()),
    )

    // Fortunate Freezing
    addNotNull(SbPattern.fortunateFreezingBonusPattern.firstMatches(getSbLines()))

    // Fossil Dust
    addNotNull(SbPattern.fossilDustPattern.firstMatches(getSbLines()))
}

private fun getDamageLines(): List<String> = listOf(
    SbPattern.bossHPPattern,
    SbPattern.bossDamagePattern,
).allMatches(getSbLines())

private fun getMagmaBossLines() = listOf(
    SbPattern.magmaBossPattern,
    SbPattern.damageSoakedPattern,
    SbPattern.killMagmasPattern,
    SbPattern.killMagmasDamagedSoakedBarPattern,
    SbPattern.reformingPattern,
    SbPattern.bossHealthPattern,
    SbPattern.bossHealthBarPattern,
).allMatches(getSbLines())

private fun getMagmaBossShowWhen(): Boolean = inAnyIsland(IslandType.CRIMSON_ISLE) &&
    SbPattern.magmaChamberPattern.matches(HypixelData.skyBlockArea)

private fun getCarnivalLines() = listOf(
    SbPattern.carnivalPattern,
    SbPattern.carnivalTokensPattern,
    SbPattern.carnivalTasksPattern,
    SbPattern.timeLeftPattern,
    SbPattern.carnivalCatchStreakPattern,
    SbPattern.carnivalFruitsPattern,
    SbPattern.carnivalAccuracyPattern,
    SbPattern.carnivalKillsPattern,
    SbPattern.carnivalScorePattern,
)
    .mapNotNull { pattern ->
        getSbLines().firstOrNull { pattern.matches(it) }
    }

private fun getCarnivalShowWhen(): Boolean = SbPattern.carnivalPattern.anyMatches(getSbLines())

private fun getRiftLines() = listOf(
    RiftBloodEffigies.heartsPattern,
    SbPattern.riftHotdogTitlePattern,
    SbPattern.timeLeftPattern,
    SbPattern.riftHotdogEatenPattern,
    SbPattern.riftAveikxPattern,
    SbPattern.riftHayEatenPattern,
    SbPattern.cluesPattern,
).allMatches(getSbLines())

private fun getEssenceLines() = listOfNotNull(SbPattern.essencePattern.firstMatches(getSbLines()))

private fun getQueueLines(): List<String> = listOf(
    SbPattern.queuePattern,
    SbPattern.queuePositionPattern,
).allMatches(getSbLines())

private fun getRedstoneLines() = listOfNotNull(SbPattern.redstonePattern.firstMatches(getSbLines()))

private fun getAnniversaryLines() = listOf(getSbLines().first { SbPattern.anniversaryPattern.matches(it) })

private fun getAnniversaryShowWhen(): Boolean = SbPattern.anniversaryPattern.anyMatches(getSbLines())
