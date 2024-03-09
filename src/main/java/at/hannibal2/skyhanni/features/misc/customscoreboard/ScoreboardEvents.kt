package at.hannibal2.skyhanni.features.misc.customscoreboard

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.garden.contest.FarmingContestAPI.sidebarCropPattern
import at.hannibal2.skyhanni.features.misc.ServerRestartTitle
import at.hannibal2.skyhanni.features.misc.customscoreboard.ScoreboardEvents.VOTING
import at.hannibal2.skyhanni.features.misc.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.features.rift.area.stillgorechateau.RiftBloodEffigies
import at.hannibal2.skyhanni.utils.CollectionUtils.addIfNotNull
import at.hannibal2.skyhanni.utils.CollectionUtils.nextAfter
import at.hannibal2.skyhanni.utils.LorenzUtils.inDungeons
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.StringUtils.anyMatches
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.TabListData
import java.util.function.Supplier
import at.hannibal2.skyhanni.features.misc.customscoreboard.ScoreboardPattern as SbPattern

private val config get() = SkyHanniMod.feature.gui.customScoreboard

/**
 * This enum contains all the lines that either are events or other lines that are so rare/not often seen that they
 * don't fit in the normal [ScoreboardElements] enum.
 *
 * We for example have the [VOTING] Event, while this is clearly not an event, I don't consider them as normal lines
 * because they are visible for a maximum of like 1 minute every 5 days and ~12 hours.
 */

private fun getSbLines(): List<String> {
    return ScoreboardData.sidebarLinesFormatted
}

enum class ScoreboardEvents(private val displayLine: Supplier<List<String>>, private val showWhen: () -> Boolean) {
    VOTING(
        ::getVotingLines,
        ::getVotingShowWhen
    ),
    SERVER_CLOSE(
        ::getServerCloseLines,
        ::getServerCloseShowWhen
    ),
    DUNGEONS(
        ::getDungeonsLines,
        ::getDungeonsShowWhen
    ),
    KUUDRA(
        ::getKuudraLines,
        ::getKuudraShowWhen
    ),
    DOJO(
        ::getDojoLines,
        ::getDojoShowWhen
    ),
    DARK_AUCTION(
        ::getDarkAuctionLines,
        ::getDarkAuctionShowWhen
    ),
    JACOB_CONTEST(
        ::getJacobContestLines,
        ::getJacobContestShowWhen
    ),
    JACOB_MEDALS(
        ::getJacobMedalsLines,
        ::getJacobMedalsShowWhen
    ),
    TRAPPER(
        ::getTrapperLines,
        ::getTrapperShowWhen
    ),
    GARDEN_CLEAN_UP(
        ::getGardenCleanUpLines,
        ::getGardenCleanUpShowWhen
    ),
    GARDEN_PASTING(
        ::getGardenPastingLines,
        ::getGardenPastingShowWhen
    ),
    FLIGHT_DURATION(
        ::getFlightDurationLines,
        ::getFlightDurationShowWhen
    ),
    WINTER(
        ::getWinterLines,
        ::getWinterShowWhen
    ),
    SPOOKY(
        ::getSpookyLines,
        ::getSpookyShowWhen
    ),
    MARINA(
        ::getMarinaLines,
        ::getMarinaShowWhen
    ),
    BROODMOTHER(
        ::getBroodmotherLines,
        ::getBroodmotherShowWhen
    ),
    NEW_YEAR(
        ::getNewYearLines,
        ::getNewYearShowWhen
    ),
    ORINGO(
        ::getOringoLines,
        ::getOringoShowWhen
    ),
    MINING_EVENTS(
        ::getMiningEventsLines,
        ::getMiningEventsShowWhen
    ),
    DAMAGE(
        ::getDamageLines,
        ::getDamageShowWhen
    ),
    MAGMA_BOSS(
        ::getMagmaBossLines,
        ::getMagmaBossShowWhen
    ),
    ESSENCE(
        ::getEssenceLines,
        ::getEssenceShowWhen
    ),
    EFFIGIES(
        ::getEffigiesLines,
        ::getEffigiesShowWhen
    ),
    REDSTONE(
        ::getRedstoneLines,
        ::getRedstoneShowWhen
    ),

    NONE( // maybe use default state tablist: "Events: smth"
        ::getNoneLines,
        { false }
    );

    fun getLines(): List<String> {
        return displayLine.get()
    }

    companion object {
        fun getEvent(): List<ScoreboardEvents> {
            if (config.displayConfig.showAllActiveEvents) {
                return entries.filter { it.showWhen() }
            }
            return listOf(entries.firstOrNull { it.showWhen() } ?: NONE)
        }
    }
}

private fun getVotingLines(): List<String> {
    return buildList {
        val sbLines = getSbLines()

        sbLines.firstOrNull { SbPattern.yearVotesPattern.matches(it) }?.let { add(it) }

        if (firstOrNull { SbPattern.yearVotesPattern.matches(it) }?.let { sbLines.nextAfter(it) } == "§7Waiting for") {
            add("§7Waiting for")
            add("§7your vote...")
        } else {
            if (SbPattern.votesPattern.anyMatches(sbLines)) {
                addAll(sbLines.filter { SbPattern.votesPattern.matches(it) })
            }
        }
    }
}

private fun getVotingShowWhen(): Boolean {
    return SbPattern.yearVotesPattern.anyMatches(getSbLines())
}

private fun getServerCloseLines() = buildList {
    val matchingLine = getSbLines().first { ServerRestartTitle.restartingGreedyPattern.matches(it) }
    add(matchingLine.split("§8")[0])
}

private fun getServerCloseShowWhen(): Boolean {
    return ServerRestartTitle.restartingGreedyPattern.anyMatches(getSbLines())
}

private fun getDungeonsLines() = listOf(
    getSbLines().firstOrNull { SbPattern.autoClosingPattern.matches(it) },
    getSbLines().firstOrNull { SbPattern.startingInPattern.matches(it) },
    getSbLines().firstOrNull { SbPattern.keysPattern.matches(it) },
    // BetterMap adds a random §r at the start, making it go black
    getSbLines().firstOrNull { SbPattern.timeElapsedPattern.matches(it) }?.replace("§r", ""),
    getSbLines().firstOrNull { SbPattern.clearedPattern.matches(it) },
    getSbLines().firstOrNull { SbPattern.soloPattern.matches(it) },
    *getSbLines().filter { SbPattern.teammatesPattern.matches(it) }.toTypedArray(),
    *getSbLines().filter { SbPattern.floor3GuardiansPattern.matches(it) }.toTypedArray()
).mapNotNull { it }

private fun getDungeonsShowWhen(): Boolean {
    return IslandType.CATACOMBS.isInIsland() || inDungeons
}

private fun getKuudraLines() = listOf(
    SbPattern.autoClosingPattern,
    SbPattern.startingInPattern,
    SbPattern.timeElapsedPattern,
    SbPattern.instanceShutdownPattern,
    SbPattern.wavePattern,
    SbPattern.tokensPattern,
    SbPattern.submergesPattern
)
    .mapNotNull { pattern ->
        getSbLines().firstOrNull { pattern.matches(it) }
    }

private fun getKuudraShowWhen(): Boolean {
    return IslandType.KUUDRA_ARENA.isInIsland()
}

private fun getDojoLines() = listOf(
    SbPattern.dojoChallengePattern,
    SbPattern.dojoDifficultyPattern,
    SbPattern.dojoPointsPattern,
    SbPattern.dojoTimePattern
)
    .mapNotNull { pattern ->
        getSbLines().firstOrNull { pattern.matches(it) }
    }

private fun getDojoShowWhen(): Boolean {
    return SbPattern.dojoChallengePattern.anyMatches(getSbLines())
}

private fun getDarkAuctionLines(): List<String> {
    return buildList {
        getSbLines().firstOrNull { SbPattern.startingInPattern.matches(it) }?.let { add(it) }
        getSbLines().firstOrNull { SbPattern.timeLeftPattern.matches(it) }?.let { add(it) }

        if (getSbLines().any { SbPattern.darkAuctionCurrentItemPattern.matches(it) }) {
            addIfNotNull(getSbLines().firstOrNull { SbPattern.darkAuctionCurrentItemPattern.matches(it) })
            addIfNotNull(
                getSbLines().firstOrNull { SbPattern.darkAuctionCurrentItemPattern.matches(it) }?.let {
                    getSbLines().nextAfter(it)
                } ?: "§7No Item"
            )
        }
    }
}

private fun getDarkAuctionShowWhen(): Boolean {
    return IslandType.DARK_AUCTION.isInIsland()
}

private fun getJacobContestLines() = buildList {
    val jacobsContestLine = getSbLines().firstOrNull { SbPattern.jacobsContestPattern.matches(it) }
        ?: "§eJacob's Contest"

    add(jacobsContestLine)
    addIfNotNull(getSbLines().nextAfter(jacobsContestLine))
    addIfNotNull(getSbLines().nextAfter(jacobsContestLine, 2))
    addIfNotNull(getSbLines().nextAfter(jacobsContestLine, 3))
}

private fun getJacobContestShowWhen(): Boolean {
    return sidebarCropPattern.anyMatches(getSbLines())
}

private fun getJacobMedalsLines(): List<String> {
    return getSbLines().filter { SbPattern.medalsPattern.matches(it) }
}

private fun getJacobMedalsShowWhen(): Boolean {
    return SbPattern.medalsPattern.anyMatches(getSbLines())
}

private fun getTrapperLines() = buildList {
    addIfNotNull(getSbLines().firstOrNull { SbPattern.peltsPattern.matches(it) })

    if (getSbLines().any { SbPattern.mobLocationPattern.matches(it) }) {
        add("Tracker Mob Location:")
        addIfNotNull(getSbLines().nextAfter(ScoreboardData.sidebarLinesFormatted.first {
            SbPattern.mobLocationPattern.matches(it)
        }) ?: "<hidden>")
    }
}

private fun getTrapperShowWhen(): Boolean {
    return getSbLines().any {
        ScoreboardPattern.peltsPattern.matches(it) || ScoreboardPattern.mobLocationPattern.matches(it)
    }
}

private fun getGardenCleanUpLines(): List<String> {
    return listOf(getSbLines().first { SbPattern.cleanUpPattern.matches(it) }.trim())
}

private fun getGardenCleanUpShowWhen(): Boolean {
    return SbPattern.cleanUpPattern.anyMatches(getSbLines())
}

private fun getGardenPastingLines(): List<String> {
    return listOf(getSbLines().first { SbPattern.pastingPattern.matches(it) }.trim())
}

private fun getGardenPastingShowWhen(): Boolean {
    return SbPattern.pastingPattern.anyMatches(getSbLines())
}

private fun getFlightDurationLines(): List<String> {
    return listOf(getSbLines().first { SbPattern.flightDurationPattern.matches(it) }.trim())
}

private fun getFlightDurationShowWhen(): Boolean {
    return SbPattern.flightDurationPattern.anyMatches(getSbLines())
}

private fun getWinterLines() = buildList {
    addIfNotNull(getSbLines().firstOrNull { SbPattern.winterEventStartPattern.matches(it) })
    addIfNotNull(getSbLines().firstOrNull { SbPattern.winterNextWavePattern.matches(it) && !it.endsWith("Soon!") })
    addIfNotNull(getSbLines().firstOrNull { SbPattern.winterWavePattern.matches(it) })
    addIfNotNull(getSbLines().firstOrNull { SbPattern.winterMagmaLeftPattern.matches(it) })
    addIfNotNull(getSbLines().firstOrNull { SbPattern.winterTotalDmgPattern.matches(it) })
    addIfNotNull(getSbLines().firstOrNull { SbPattern.winterCubeDmgPattern.matches(it) })
}

private fun getWinterShowWhen(): Boolean {
    return getSbLines().any { ScoreboardPattern.winterEventStartPattern.matches(it) }
        || getSbLines().any { ScoreboardPattern.winterNextWavePattern.matches(it) && !it.endsWith("Soon!") }
        || getSbLines().any { ScoreboardPattern.winterWavePattern.matches(it) }
}

private fun getSpookyLines() = buildList {
    addIfNotNull(getSbLines().firstOrNull { SbPattern.spookyPattern.matches(it) }) // Time
    addIfNotNull("§7Your Candy: ")
    addIfNotNull(
        CustomScoreboardUtils.getTablistFooter()
            .split("\n")
            .firstOrNull { it.startsWith("§7Your Candy:") }
            ?.removePrefix("§7Your Candy:") ?: "§cCandy not found"
    ) // Candy
}

private fun getSpookyShowWhen(): Boolean {
    return getSbLines().any { ScoreboardPattern.spookyPattern.matches(it) }
}

private fun getMarinaLines(): List<String> {
    return listOf(
        "§bFishing Festival: " + TabListData.getTabList().nextAfter("§e§lEvent: §r§bFishing Festival")
            ?.removePrefix(" Ends In: ")
    )
}

private fun getMarinaShowWhen(): Boolean {
    return TabListData.getTabList()
        .any { it.startsWith("§e§lEvent: §r§bFishing Festival") } && TabListData.getTabList()
        .nextAfter("§e§lEvent: §r§bFishing Festival")?.startsWith(" Ends In: ") == true
}

private fun getBroodmotherLines(): List<String> {
    return listOf(getSbLines().first { SbPattern.broodmotherPattern.matches(it) })
}

private fun getBroodmotherShowWhen(): Boolean {
    return getSbLines().any { SbPattern.broodmotherPattern.matches(it) }
}

private fun getNewYearLines(): List<String> {
    return listOf(getSbLines().first { SbPattern.newYearPattern.matches(it) })
}

private fun getNewYearShowWhen(): Boolean {
    return getSbLines().any { SbPattern.newYearPattern.matches(it) }
}

private fun getOringoLines(): List<String> {
    return listOf(getSbLines().first { SbPattern.travelingZooPattern.matches(it) })
}

private fun getOringoShowWhen(): Boolean {
    return getSbLines().any { SbPattern.travelingZooPattern.matches(it) }
}

private fun getMiningEventsLines() = buildList {
    // Mining Fiesta
    if (TabListData.getTabList().any { it == "§e§lEvent: §r§6Mining Fiesta" }
        && TabListData.getTabList().nextAfter("§e§lEvent: §r§6Mining Fiesta")
            ?.startsWith(" Ends In:") == true) {
        add(
            "§6Mining Fiesta: " + TabListData.getTabList().nextAfter("§e§lEvent: §r§6Mining Fiesta")
                ?.removePrefix(" Ends In: ")
        )
    }

    // Wind
    getSbLines().firstOrNull { SbPattern.windCompassPattern.matches(it) }?.let {
        add(it)
        add("| ${getSbLines().first { SbPattern.windCompassArrowPattern.matches(it) }} §f|")
    }

    // Better Together
    getSbLines().firstOrNull { SbPattern.nearbyPlayersPattern.matches(it) }?.let {
        add("§dBetter Together")
        add(" $it")
    }

    // Zone Events
    if (getSbLines().any { SbPattern.miningEventPattern.matches(it) }
        && getSbLines().any { SbPattern.miningEventZonePattern.matches(it) }) {
        add(getSbLines().first { SbPattern.miningEventPattern.matches(it) }.removePrefix("Event: "))
        add("in ${getSbLines().first { SbPattern.miningEventZonePattern.matches(it) }.removePrefix("Zone: ")}")
    }

    // Zone Events but no Zone Line
    if (getSbLines().any { SbPattern.miningEventPattern.matches(it) }
        && getSbLines().none { SbPattern.miningEventZonePattern.matches(it) }) {
        add(getSbLines().first { SbPattern.miningEventPattern.matches(it) }
            .removePrefix("Event: "))
    }

    // Mithril Gourmand
    if (getSbLines().any { SbPattern.mithrilRemainingPattern.matches(it) }
        && getSbLines().any { SbPattern.mithrilYourMithrilPattern.matches(it) }) {
        add(getSbLines().first { SbPattern.mithrilRemainingPattern.matches(it) })
        add(getSbLines().first { SbPattern.mithrilYourMithrilPattern.matches(it) })
    }

    // Raffle
    if (getSbLines().any { SbPattern.raffleTicketsPattern.matches(it) }
        && getSbLines().any { SbPattern.rafflePoolPattern.matches(it) }) {
        add(getSbLines().first { SbPattern.raffleTicketsPattern.matches(it) })
        add(getSbLines().first { SbPattern.rafflePoolPattern.matches(it) })
    }

    // Raid
    if (getSbLines().any { SbPattern.yourGoblinKillsPattern.matches(it) }
        && getSbLines().any { SbPattern.remainingGoblinPattern.matches(it) }) {
        add(getSbLines().first { SbPattern.yourGoblinKillsPattern.matches(it) })
        add(getSbLines().first { SbPattern.remainingGoblinPattern.matches(it) })
    }
}

private fun getMiningEventsShowWhen(): Boolean {
    return IslandType.DWARVEN_MINES.isInIsland() || IslandType.CRYSTAL_HOLLOWS.isInIsland()
}

private fun getDamageLines(): List<String> {
    return listOf(getSbLines().first { SbPattern.bossHPPattern.matches(it) }) +
        (getSbLines().first { SbPattern.bossDamagePattern.matches(it) })
}

private fun getDamageShowWhen(): Boolean {
    return getSbLines().any { SbPattern.bossHPPattern.matches(it) || SbPattern.bossDamagePattern.matches(it) }
}

private fun getMagmaBossLines() = listOf(
    SbPattern.magmaBossPattern,
    SbPattern.damageSoakedPattern,
    SbPattern.killMagmasPattern,
    SbPattern.killMagmasDamagedSoakedBarPattern,
    SbPattern.reformingPattern,
    SbPattern.bossHealthPattern,
    SbPattern.bossHealthBarPattern
)
    .mapNotNull { pattern ->
        getSbLines().firstOrNull { pattern.matches(it) }
    }

private fun getMagmaBossShowWhen(): Boolean {
    return HypixelData.skyBlockArea == "Magma Chamber"
}

private fun getEssenceLines(): List<String> {
    return listOf(getSbLines().first { SbPattern.essencePattern.matches(it) })
}

private fun getEssenceShowWhen(): Boolean {
    return SbPattern.essencePattern.anyMatches(getSbLines())
}

private fun getEffigiesLines(): List<String> {
    return listOf(getSbLines().first { RiftBloodEffigies.heartsPattern.matches(it) })
}

private fun getEffigiesShowWhen(): Boolean {
    return RiftBloodEffigies.heartsPattern.anyMatches(getSbLines())
}

private fun getRedstoneLines(): List<String> {
    return listOf(getSbLines().first { SbPattern.redstonePattern.matches(it) })
}

private fun getRedstoneShowWhen(): Boolean {
    return SbPattern.redstonePattern.anyMatches(getSbLines())
}

private fun getNoneLines(): List<String> {
    return when {
        config.informationFilteringConfig.hideEmptyLines -> listOf("<hidden>")
        else -> listOf("§cNo Event")
    }
}
