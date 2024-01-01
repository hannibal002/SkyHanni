package at.hannibal2.skyhanni.features.misc.customscoreboard

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.misc.ServerRestartTitle
import at.hannibal2.skyhanni.features.misc.customscoreboard.ScoreboardEvents.VOTING
import at.hannibal2.skyhanni.features.misc.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.features.rift.area.stillgorechateau.RiftBloodEffigies
import at.hannibal2.skyhanni.utils.LorenzUtils.inDungeons
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.LorenzUtils.nextAfter
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
    DARK_AUCTION( // This will use regex once PR #823 is merged
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
    val list = mutableListOf<String>()

    getSbLines().firstOrNull { SbPattern.yearVotesPattern.matches(it) }?.let { list.add(it) }

    if (getSbLines().nextAfter(list[0]) == "§7Waiting for") {
        list += "§7Waiting for"
        list += "§7your vote..."
    } else {
        if (getSbLines().any { SbPattern.votesPattern.matches(it) }) {
            list += getSbLines().filter { SbPattern.votesPattern.matches(it) }
        }
    }

    return list
}

private fun getVotingShowWhen(): Boolean {
    return getSbLines().any { SbPattern.yearVotesPattern.matches(it) }
}

private fun getServerCloseLines(): List<String> {
    return listOf(getSbLines().first { ServerRestartTitle.restartingPattern.matches(it) })
}

private fun getServerCloseShowWhen(): Boolean {
    return getSbLines().any { ServerRestartTitle.restartingPattern.matches(it) }
}

private fun getDungeonsLines(): List<String> {
    val list = mutableListOf<String>()

    getSbLines().firstOrNull { SbPattern.autoClosingPattern.matches(it) }?.let { list.add(it) }
    getSbLines().firstOrNull { SbPattern.startingInPattern.matches(it) }?.let { list.add(it) }
    getSbLines().firstOrNull { SbPattern.keysPattern.matches(it) }?.let { list.add(it) }
    getSbLines().firstOrNull { SbPattern.timeElapsedPattern.matches(it) }?.let { list.add(it) }
    // BetterMap adds a random §r at the start, making it go black
    getSbLines().firstOrNull { SbPattern.clearedPattern.matches(it) }?.let { list.add(it.replace("§r", "")) }
    getSbLines().firstOrNull { SbPattern.soloPattern.matches(it) }?.let { list.add(it) }
    list.addAll(getSbLines().filter { SbPattern.teammatesPattern.matches(it) })
    list.addAll(getSbLines().filter { SbPattern.floor3GuardiansPattern.matches(it) })

    return list
}

private fun getDungeonsShowWhen(): Boolean {
    return IslandType.CATACOMBS.isInIsland() || inDungeons
}

private fun getKuudraLines(): List<String> {
    val list = mutableListOf<String>()

    getSbLines().firstOrNull { SbPattern.autoClosingPattern.matches(it) }?.let { list.add(it) }
    getSbLines().firstOrNull { SbPattern.startingInPattern.matches(it) }?.let { list.add(it) }
    getSbLines().firstOrNull { SbPattern.timeElapsedPattern.matches(it) }?.let { list.add(it) }

    if (getSbLines().any { it.startsWith("Instance Shutdow") }) {
        list += getSbLines().firstOrNull { it.startsWith("Instance Shutdow") }
            ?: "<hidden>"
    }
    list += ""
    if (getSbLines().any { it.startsWith("§f§lWave: §c§l") }) {
        list += getSbLines().firstOrNull { it.startsWith("§f§lWave: §c§l") }
            ?: "<hidden>"
    }
    if (getSbLines().any { it.startsWith("§fTokens: ") }) {
        list += getSbLines().firstOrNull { it.startsWith("§fTokens: ") } ?: "<hidden>"
    }
    if (getSbLines().any { it.startsWith("Submerges In: §e") }) {
        list += getSbLines().firstOrNull { it.startsWith("Submerges In: §e") }
            ?: "<hidden>"
    }
    list += ""
    if (getSbLines().any { it == "§fObjective:" }) {
        list += "§fObjective:"
        list += getSbLines().nextAfter("§fObjective:") ?: "§cNo Objective"
        if (extraObjectiveKuudraLines.any {
                it == getSbLines().nextAfter(
                    "§fObjective:",
                    2
                )
            }) {
            list += getSbLines().nextAfter("§fObjective:", 2) ?: "§cNo Objective"
        }
    }

    return list
}

private fun getKuudraShowWhen(): Boolean {
    return IslandType.KUUDRA_ARENA.isInIsland()
}

private fun getDojoLines(): List<String> {
    val list = mutableListOf<String>()

    getSbLines().firstOrNull { SbPattern.dojoChallengePattern.matches(it) }?.let { list.add(it) }
    getSbLines().firstOrNull { SbPattern.dojoDifficultyPattern.matches(it) }?.let { list.add(it) }
    getSbLines().firstOrNull { SbPattern.dojoPointsPattern.matches(it) }?.let { list.add(it) }
    getSbLines().firstOrNull { SbPattern.dojoTimePattern.matches(it) }?.let { list.add(it) }

    return list
}

private fun getDojoShowWhen(): Boolean {
    return getSbLines().any { ScoreboardPattern.dojoChallengePattern.matches(it) }
}

private fun getDarkAuctionLines(): List<String> {
    val list = mutableListOf<String>()
    if (getSbLines().any { it.startsWith("Time Left: §b") }) {
        list += getSbLines().firstOrNull { it.startsWith("Time Left: §b") }
            ?: "<hidden>"
    }
    list += "Current Item:"
    list += getSbLines().nextAfter("Current Item:") ?: "<hidden>"

    return list
}

private fun getDarkAuctionShowWhen(): Boolean {
    return getSbLines().any { it == "Current Item:" }
}

private fun getJacobContestLines(): List<String> {
    val list = mutableListOf<String>()

    list += "§eJacob's Contest"
    list += getSbLines().nextAfter("§eJacob's Contest") ?: "§7No Event"
    list += getSbLines().nextAfter("§eJacob's Contest", 2) ?: "§7No Ranking"
    list += getSbLines().nextAfter("§eJacob's Contest", 3) ?: "§7No Amount for next"

    return list
}

private fun getJacobContestShowWhen(): Boolean {
    return getSbLines().any { it.startsWith("§e○ §f") || it.startsWith("§6☘ §f") }
}

private fun getJacobMedalsLines(): List<String> {
    return getSbLines().filter { SbPattern.medalsPattern.matches(it) }
}

private fun getJacobMedalsShowWhen(): Boolean {
    return getSbLines().any { SbPattern.medalsPattern.matches(it) }
}

private fun getTrapperLines(): List<String> {
    val list = mutableListOf<String>()

    getSbLines().firstOrNull { SbPattern.peltsPattern.matches(it) }?.let { list.add(it) }

    if (getSbLines().any { SbPattern.mobLocationPattern.matches(it) }) {
        list += "Tracker Mob Location:"
        list += getSbLines().nextAfter(ScoreboardData.sidebarLinesFormatted.first {
            ScoreboardPattern.mobLocationPattern.matches(it)
        }) ?: "<hidden>"
    }

    return list
}

private fun getTrapperShowWhen(): Boolean {
    return getSbLines().any {
        ScoreboardPattern.peltsPattern.matches(it) || ScoreboardPattern.mobLocationPattern.matches(
            it
        )
    }
}

private fun getGardenCleanUpLines(): List<String> {
    return listOf(getSbLines().first { SbPattern.cleanUpPattern.matches(it) })
}

private fun getGardenCleanUpShowWhen(): Boolean {
    return getSbLines().any { SbPattern.cleanUpPattern.matches(it) }
}

private fun getGardenPastingLines(): List<String> {
    return listOf(getSbLines().first { SbPattern.pastingPattern.matches(it) })
}

private fun getGardenPastingShowWhen(): Boolean {
    return getSbLines().any { SbPattern.pastingPattern.matches(it) }
}

private fun getFlightDurationLines(): List<String> {
    return listOf(
        getSbLines().first { SbPattern.flightDurationPattern.matches(it) },
    )
}

private fun getFlightDurationShowWhen(): Boolean {
    return getSbLines().any { SbPattern.flightDurationPattern.matches(it) }
}

private fun getWinterLines(): List<String> {
    val list = mutableListOf<String>()

    getSbLines().firstOrNull { SbPattern.winterEventStartPattern.matches(it) }?.let { list.add(it) }
    getSbLines().firstOrNull { SbPattern.winterNextWavePattern.matches(it) && !it.endsWith("Soon!") }?.let { list.add(it) }
    getSbLines().firstOrNull { SbPattern.winterWavePattern.matches(it) }?.let { list.add(it) }
    getSbLines().firstOrNull { SbPattern.winterMagmaLeftPattern.matches(it) }?.let { list.add(it) }
    getSbLines().firstOrNull { SbPattern.winterTotalDmgPattern.matches(it) }?.let { list.add(it) }
    getSbLines().firstOrNull { SbPattern.winterCubeDmgPattern.matches(it) }?.let { list.add(it) }

    return list
}

private fun getWinterShowWhen(): Boolean {
    return getSbLines().any { ScoreboardPattern.winterEventStartPattern.matches(it) }
        || getSbLines().any { ScoreboardPattern.winterNextWavePattern.matches(it) && !it.endsWith("Soon!") }
        || getSbLines().any { ScoreboardPattern.winterWavePattern.matches(it) }
}

private fun getSpookyLines(): List<String> {
    return listOf(getSbLines().first { ScoreboardPattern.spookyPattern.matches(it) }) + // Time
        ("§7Your Candy: ") +
        (CustomScoreboardUtils.getTablistFooter().split("\n").firstOrNull { it.startsWith("§7Your Candy:") }
            ?.removePrefix("§7Your Candy:") ?: "§cCandy not found") // Candy
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

private fun getMiningEventsLines(): List<String> {
    val list = mutableListOf<String>()

    // Mining Fiesta
    if (TabListData.getTabList().any { it == "§e§lEvent: §r§6Mining Fiesta" }
        && TabListData.getTabList().nextAfter("§e§lEvent: §r§6Mining Fiesta")
            ?.startsWith(" Ends In:") == true) {
        list += "§6Mining Fiesta: " + TabListData.getTabList().nextAfter("§e§lEvent: §r§6Mining Fiesta")
            ?.removePrefix(" Ends In: ")
    }

    // Wind
    if (getSbLines().any { SbPattern.windCompassPattern.matches(it) }) {
        list += getSbLines().first { SbPattern.windCompassPattern.matches(it) }
        list += ("|" + getSbLines().first { SbPattern.windCompassArrowPattern.matches(it) } + "|")
    }

    // Better Together
    if (getSbLines().any { ScoreboardPattern.nearbyPlayersPattern.matches(it) }) {
        list += "§dBetter Together"
        list += (" " + getSbLines().first { ScoreboardPattern.nearbyPlayersPattern.matches(it) })
    }

    // Zone Events
    if (
        getSbLines().any { ScoreboardPattern.miningEventPattern.matches(it) }
        && getSbLines().any { ScoreboardPattern.miningEventZonePattern.matches(it) }
    ) {
        list += getSbLines().first { ScoreboardPattern.miningEventPattern.matches(it) }
            .removePrefix("Event: ")
        list += "in " + getSbLines().first { ScoreboardPattern.miningEventZonePattern.matches(it) }
            .removePrefix("Zone: ")
    }

    // Zone Events but no Zone Line
    if (
        getSbLines().any { ScoreboardPattern.miningEventPattern.matches(it) }
        && getSbLines().none { ScoreboardPattern.miningEventZonePattern.matches(it) }
    ) {
        list += getSbLines().first { ScoreboardPattern.miningEventPattern.matches(it) }
            .removePrefix("Event: ")
    }

    // Mithril Gourmand
    if (
        getSbLines().any { ScoreboardPattern.mithrilRemainingPattern.matches(it) }
        && getSbLines().any { ScoreboardPattern.mithrilYourMithrilPattern.matches(it) }
    ) {
        list += getSbLines().first { ScoreboardPattern.mithrilRemainingPattern.matches(it) }
        list += getSbLines().first { ScoreboardPattern.mithrilYourMithrilPattern.matches(it) }
    }

    // raffle
    if (
        getSbLines().any { ScoreboardPattern.raffleTicketsPattern.matches(it) }
        && getSbLines().any { ScoreboardPattern.rafflePoolPattern.matches(it) }
    ) {
        list += getSbLines().first { ScoreboardPattern.raffleTicketsPattern.matches(it) }
        list += getSbLines().first { ScoreboardPattern.rafflePoolPattern.matches(it) }
    }

    // raid
    if (
        getSbLines().any { ScoreboardPattern.yourGoblinKillsPattern.matches(it) }
        && getSbLines().any { ScoreboardPattern.remainingGoblinPattern.matches(it) }
    ) {
        list += getSbLines().first { ScoreboardPattern.yourGoblinKillsPattern.matches(it) }
        list += getSbLines().first { ScoreboardPattern.remainingGoblinPattern.matches(it) }
    }

    return if (list.size == 0) when (config.informationFilteringConfig.hideEmptyLines) {
        true -> listOf("<hidden>")
        false -> listOf("§cNo Mining Event")
    } else list
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

private fun getMagmaBossLines(): List<String> {
    val list = listOf(
        SbPattern.magmaBossPattern,
        SbPattern.damageSoakedPattern,
        SbPattern.damagedSoakedBarPattern,
        SbPattern.killMagmasPattern,
        SbPattern.killMagmasBarPattern,
        SbPattern.reformingPattern,
        SbPattern.bossHealthPattern,
        SbPattern.bossHealthBarPattern
    )
        .mapNotNull { pattern ->
            getSbLines().firstOrNull { pattern.matches(it) }
        }

    return list.ifEmpty {
        when (config.informationFilteringConfig.hideEmptyLines) {
            true -> listOf("<hidden>")
            false -> listOf("§cNo Mining Event")
        }
    }
}

private fun getMagmaBossShowWhen(): Boolean {
    return at.hannibal2.skyhanni.data.HypixelData.skyBlockArea == "Magma Chamber"
}

private fun getEssenceLines(): List<String> {
    return listOf(getSbLines().first { SbPattern.essencePattern.matches(it) })
}

private fun getEssenceShowWhen(): Boolean {
    return getSbLines().any { SbPattern.essencePattern.matches(it) }
}

private fun getEffigiesLines(): List<String> {
    return listOf(getSbLines().first { RiftBloodEffigies.heartsPattern.matches(it) })
}

private fun getEffigiesShowWhen(): Boolean {
    return getSbLines().any { RiftBloodEffigies.heartsPattern.matches(it) }
}

private fun getRedstoneLines(): List<String> {
    return listOf(getSbLines().first { SbPattern.redstonePattern.matches(it) })
}

private fun getRedstoneShowWhen(): Boolean {
    return getSbLines().any { SbPattern.redstonePattern.matches(it) }
}

private fun getNoneLines(): List<String> {
    return when {
        config.informationFilteringConfig.hideEmptyLines -> listOf("<hidden>")
        else -> listOf("§cNo Event")
    }
}
