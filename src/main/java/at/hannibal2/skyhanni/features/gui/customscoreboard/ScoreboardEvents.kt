package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.Companion.eventsConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardEvents.VOTING
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.features.misc.ServerRestartTitle
import at.hannibal2.skyhanni.features.rift.area.stillgorechateau.RiftBloodEffigies
import at.hannibal2.skyhanni.utils.CollectionUtils.nextAfter
import at.hannibal2.skyhanni.utils.LorenzUtils.inAdvancedMiningIsland
import at.hannibal2.skyhanni.utils.LorenzUtils.inDungeons
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.StringUtils.anyMatches
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
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
        "§7(All Spooky Event Lines)"
    ),
    BROODMOTHER(
        ::getBroodmotherLines,
        ::getBroodmotherShowWhen,
        "§4Broodmother§7: §eDormant"
    ),
    MINING_EVENTS(
        ::getMiningEventsLines,
        ::getMiningEventsShowWhen,
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
    HOT_DOG_CONTEST(
        ::getHotDogLines,
        ::getHotDogShowWhen,
        "§6Hot Dog Contest\n" +
            "Eaten: §c0/50"
    ),
    ESSENCE(
        ::getEssenceLines,
        ::getEssenceShowWhen,
        "Dragon Essence: §d1,285"
    ),
    EFFIGIES(
        ::getEffigiesLines,
        ::getEffigiesShowWhen,
        "Effigies: §c⧯§c⧯⧯§7⧯§c⧯§c⧯"
    ),
    ACTIVE_TABLIST_EVENTS(
        ::getActiveEventLine,
        ::getActiveEventShowWhen,
        "§7(All Active Tablist Events)"
    ),
    REDSTONE(
        ::getRedstoneLines,
        ::getRedstoneShowWhen,
        "§e§l⚡ §cRedstone: §e§b7%"
    ),
    ;

    override fun toString(): String {
        return configLine
    }

    fun getLines(): List<String> {
        return displayLine.get()
    }

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
            HOT_DOG_CONTEST,
            ESSENCE,
            EFFIGIES,
            ACTIVE_TABLIST_EVENTS
        )
    }
}

private fun getVotingLines() = buildList {
    val sbLines = getSbLines()

    val yearLine = sbLines.firstOrNull { SbPattern.yearVotesPattern.matches(it) } ?: return emptyList<String>()
    add(yearLine)

    if (sbLines.nextAfter(yearLine) == "§7Waiting for") {
        add("§7Waiting for")
        add("§7your vote...")
    } else {
        if (SbPattern.votesPattern.anyMatches(sbLines)) {
            addAll(sbLines.filter { SbPattern.votesPattern.matches(it) })
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
    SbPattern.m7dragonsPattern,
    SbPattern.autoClosingPattern,
    SbPattern.startingInPattern,
    SbPattern.keysPattern,
    SbPattern.timeElapsedPattern,
    SbPattern.clearedPattern,
    SbPattern.soloPattern,
    SbPattern.teammatesPattern,
    SbPattern.floor3GuardiansPattern
).let { patterns ->
    // BetterMap adds a random §r at the start, making it go black
    getSbLines().filter { line -> patterns.any { it.matches(line) } }.map { it.removePrefix("§r") }
}

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

private fun getDarkAuctionLines() = buildList {
    getSbLines().firstOrNull { SbPattern.startingInPattern.matches(it) }?.let { add(it) }
    getSbLines().firstOrNull { SbPattern.timeLeftPattern.matches(it) }?.let { add(it) }

    val darkAuctionCurrentItemLine = getSbLines().firstOrNull { SbPattern.darkAuctionCurrentItemPattern.matches(it) }

    if (darkAuctionCurrentItemLine != null) {
        add(darkAuctionCurrentItemLine)
        getSbLines().nextAfter(darkAuctionCurrentItemLine)?.let { add(it) }
    }
}

private fun getDarkAuctionShowWhen(): Boolean {
    return IslandType.DARK_AUCTION.isInIsland()
}

private fun getJacobContestLines() = buildList {
    getSbLines().firstOrNull { SbPattern.jacobsContestPattern.matches(it) }?.let { line ->
        add(line)
        getSbLines().nextAfter(line)?.let { add(it) }
        getSbLines().nextAfter(line, 2)?.let { add(it) }
        getSbLines().nextAfter(line, 3)?.let {
            if (!SbPattern.footerPattern.matches(it)) add(it)
        }
    }
}

private fun getJacobContestShowWhen(): Boolean {
    return SbPattern.jacobsContestPattern.anyMatches(getSbLines())
}

private fun getJacobMedalsLines(): List<String> {
    return getSbLines().filter { SbPattern.medalsPattern.matches(it) }
}

private fun getJacobMedalsShowWhen(): Boolean {
    return SbPattern.medalsPattern.anyMatches(getSbLines())
}

private fun getTrapperLines() = buildList {
    getSbLines().firstOrNull { SbPattern.peltsPattern.matches(it) }?.let { add(it) }

    val trapperMobLocationLine = getSbLines().firstOrNull { SbPattern.mobLocationPattern.matches(it) }
    if (trapperMobLocationLine != null) {
        add("Tracker Mob Location:")
        getSbLines().nextAfter(trapperMobLocationLine)?.let { add(it) }
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
    getSbLines().firstOrNull { SbPattern.winterEventStartPattern.matches(it) }?.let { add(it) }
    getSbLines().firstOrNull { SbPattern.winterNextWavePattern.matches(it) && !it.endsWith("Soon!") }?.let { add(it) }
    getSbLines().firstOrNull { SbPattern.winterWavePattern.matches(it) }?.let { add(it) }
    getSbLines().firstOrNull { SbPattern.winterMagmaLeftPattern.matches(it) }?.let { add(it) }
    getSbLines().firstOrNull { SbPattern.winterTotalDmgPattern.matches(it) }?.let { add(it) }
    getSbLines().firstOrNull { SbPattern.winterCubeDmgPattern.matches(it) }?.let { add(it) }
}

private fun getWinterShowWhen(): Boolean {
    return getSbLines().any {
        ScoreboardPattern.winterEventStartPattern.matches(it)
            || (ScoreboardPattern.winterNextWavePattern.matches(it) && !it.endsWith("Soon!"))
            || ScoreboardPattern.winterWavePattern.matches(it)
    }
}

private fun getSpookyLines() = buildList {
    getSbLines().firstOrNull { SbPattern.spookyPattern.matches(it) }?.let { add(it) } // Time
    add("§7Your Candy: ")
    add(
        CustomScoreboardUtils.getTablistFooter()
            .split("\n")
            .firstOrNull { it.startsWith("§7Your Candy:") }
            ?.removePrefix("§7Your Candy:") ?: "§cCandy not found"
    ) // Candy
}

private fun getSpookyShowWhen(): Boolean {
    return getSbLines().any { ScoreboardPattern.spookyPattern.matches(it) }
}

private fun getActiveEventLine(): List<String> {
    val currentActiveEvent = TabListData.getTabList().firstOrNull { SbPattern.eventNamePattern.matches(it) }
        ?.let {
            SbPattern.eventNamePattern.matchMatcher(it) {
                group("name")
            }
        }
    val currentActiveEventEndsIn = TabListData.getTabList().firstOrNull { SbPattern.eventTimeEndsPattern.matches(it) }
        ?.let {
            SbPattern.eventTimeEndsPattern.matchMatcher(it) {
                group("time")
            }
        }

    return listOf("$currentActiveEvent $currentActiveEventEndsIn")
}

private fun getActiveEventShowWhen(): Boolean {
    return TabListData.getTabList().any { SbPattern.eventNamePattern.matches(it) } &&
        TabListData.getTabList().any { SbPattern.eventTimeEndsPattern.matches(it) }
}

private fun getBroodmotherLines(): List<String> {
    return listOf(getSbLines().first { SbPattern.broodmotherPattern.matches(it) })
}

private fun getBroodmotherShowWhen(): Boolean {
    return getSbLines().any { SbPattern.broodmotherPattern.matches(it) }
}

private fun getMiningEventsLines() = buildList {
    // Wind
    if (getSbLines().any { SbPattern.windCompassPattern.matches(it) }
        && getSbLines().any { SbPattern.windCompassArrowPattern.matches(it) }) {
        add(getSbLines().first { SbPattern.windCompassPattern.matches(it) })
        add("| ${getSbLines().first { SbPattern.windCompassArrowPattern.matches(it) }} §f|")
    }

    // Better Together
    if (getSbLines().any { SbPattern.nearbyPlayersPattern.matches(it) }) {
        add("§dBetter Together")
        add(" ${getSbLines().first { SbPattern.nearbyPlayersPattern.matches(it) }}")
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
    return inAdvancedMiningIsland()
}

private fun getDamageLines(): List<String> {
    return listOf(getSbLines().first { SbPattern.bossHPPattern.matches(it) }) +
        (getSbLines().first { SbPattern.bossDamagePattern.matches(it) })
}

private fun getDamageShowWhen(): Boolean {
    return getSbLines().any { SbPattern.bossHPPattern.matches(it) }
        && getSbLines().any { SbPattern.bossDamagePattern.matches(it) }
}

private fun getMagmaBossLines() = getSbLines().filter { line ->
    SbPattern.magmaBossPattern.matches(line)
        || SbPattern.damageSoakedPattern.matches(line)
        || SbPattern.killMagmasPattern.matches(line)
        || SbPattern.killMagmasDamagedSoakedBarPattern.matches(line)
        || SbPattern.reformingPattern.matches(line)
        || SbPattern.bossHealthPattern.matches(line)
        || SbPattern.bossHealthBarPattern.matches(line)
}

private fun getMagmaBossShowWhen(): Boolean {
    return SbPattern.magmaChamberPattern.matches(HypixelData.skyBlockArea)
}

private fun getHotDogLines(): List<String> {
    return listOf(getSbLines().first { SbPattern.riftHotdogTitlePattern.matches(it) }) +
        (getSbLines().first { SbPattern.timeLeftPattern.matches(it) }) +
        (getSbLines().first { SbPattern.riftHotdogEatenPattern.matches(it) })
}

private fun getHotDogShowWhen(): Boolean {
    return SbPattern.riftHotdogTitlePattern.anyMatches(getSbLines())
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
