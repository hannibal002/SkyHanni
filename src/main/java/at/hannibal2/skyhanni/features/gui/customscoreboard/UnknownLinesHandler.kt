package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.data.BitsAPI
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.MiningAPI
import at.hannibal2.skyhanni.data.PurseAPI
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.config
import at.hannibal2.skyhanni.features.misc.ServerRestartTitle
import at.hannibal2.skyhanni.features.rift.area.stillgorechateau.RiftBloodEffigies
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.editCopy
import at.hannibal2.skyhanni.utils.CollectionUtils.nextAfter
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SizeLimitedSet
import at.hannibal2.skyhanni.utils.StringUtils.pluralize
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.TimeLimitedSet
import com.google.common.cache.RemovalCause
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.seconds
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern as SbPattern

internal var confirmedLinesCache = SizeLimitedSet<String>(100)

internal var confirmedUnknownLines = listOf<String>()
internal var unconfirmedUnknownLines = listOf<String>()

internal var unknownLinesSet = TimeLimitedSet<String>(1.seconds) { line, cause -> if (cause == RemovalCause.EXPIRED) onRemoval(line) }

private fun onRemoval(line: String) {
    if (!LorenzUtils.inSkyBlock) return
    if (line !in unconfirmedUnknownLines) return
    unconfirmedUnknownLines = unconfirmedUnknownLines.filterNot { it == line }
    confirmedUnknownLines = confirmedUnknownLines.editCopy { add(line) }
    if (!config.unknownLinesWarning) return
    val pluralize = pluralize(confirmedUnknownLines.size, "unknown line", withNumber = true)
    val message = "CustomScoreboard detected $pluralize"
    ErrorManager.logErrorWithData(
        UndetectedScoreboardLines(message),
        message,
        "Unknown Lines" to confirmedUnknownLines,
        "Island" to LorenzUtils.skyBlockIsland,
        "Area" to HypixelData.skyBlockArea,
        "Full Scoreboard" to ScoreboardData.sidebarLinesFormatted,
        noStackTrace = true,
        betaOnly = true,
    )
}

private class UndetectedScoreboardLines(message: String) : Exception(message)

object UnknownLinesHandler {

    internal lateinit var remoteOnlyPatterns: Array<Pattern>

    fun handleUnknownLines() {
        val sidebarLines = ScoreboardData.sidebarLinesFormatted

        var unknownLines = sidebarLines
            .map { it.removeResets() }
            .filter { it.isNotBlank() }
            .filter { it.trim().length > 3 }

        // Remove known lines with patterns
        val patternsToExclude = listOf(
            PurseAPI.coinsPattern,
            SbPattern.motesPattern,
            BitsAPI.bitsScoreboardPattern,
            SbPattern.heatPattern,
            SbPattern.copperPattern,
            SbPattern.locationPattern,
            SbPattern.lobbyCodePattern,
            SbPattern.datePattern,
            SbPattern.timePattern,
            SbPattern.footerPattern,
            SbPattern.yearVotesPattern,
            SbPattern.votesPattern,
            SbPattern.waitingForVotePattern,
            SbPattern.northstarsPattern,
            SbPattern.profileTypePattern,
            SbPattern.autoClosingPattern,
            SbPattern.startingInPattern,
            SbPattern.timeElapsedPattern,
            SbPattern.instanceShutdownPattern,
            SbPattern.keysPattern,
            SbPattern.clearedPattern,
            SbPattern.soloPattern,
            SbPattern.teammatesPattern,
            SbPattern.floor3GuardiansPattern,
            SbPattern.m7dragonsPattern,
            SbPattern.wavePattern,
            SbPattern.tokensPattern,
            SbPattern.submergesPattern,
            SbPattern.medalsPattern,
            SbPattern.lockedPattern,
            SbPattern.cleanUpPattern,
            SbPattern.pastingPattern,
            SbPattern.peltsPattern,
            SbPattern.mobLocationPattern,
            SbPattern.jacobsContestPattern,
            SbPattern.plotPattern,
            SbPattern.powderGreedyPattern,
            SbPattern.windCompassPattern,
            SbPattern.windCompassArrowPattern,
            SbPattern.miningEventPattern,
            SbPattern.miningEventZonePattern,
            SbPattern.raffleUselessPattern,
            SbPattern.raffleTicketsPattern,
            SbPattern.rafflePoolPattern,
            SbPattern.mithrilUselessPattern,
            SbPattern.mithrilRemainingPattern,
            SbPattern.mithrilYourMithrilPattern,
            SbPattern.nearbyPlayersPattern,
            SbPattern.uselessGoblinPattern,
            SbPattern.remainingGoblinPattern,
            SbPattern.yourGoblinKillsPattern,
            SbPattern.magmaBossPattern,
            SbPattern.damageSoakedPattern,
            SbPattern.killMagmasPattern,
            SbPattern.killMagmasDamagedSoakedBarPattern,
            SbPattern.reformingPattern,
            SbPattern.bossHealthPattern,
            SbPattern.bossHealthBarPattern,
            SbPattern.broodmotherPattern,
            SbPattern.bossHPPattern,
            SbPattern.bossDamagePattern,
            SbPattern.slayerQuestPattern,
            SbPattern.essencePattern,
            SbPattern.redstonePattern,
            SbPattern.anniversaryPattern,
            SbPattern.visitingPattern,
            SbPattern.dojoChallengePattern,
            SbPattern.dojoDifficultyPattern,
            SbPattern.dojoPointsPattern,
            SbPattern.dojoTimePattern,
            SbPattern.objectivePattern,
            ServerRestartTitle.restartingGreedyPattern,
            SbPattern.travelingZooPattern,
            SbPattern.newYearPattern,
            SbPattern.spookyPattern,
            SbPattern.winterEventStartPattern,
            SbPattern.winterNextWavePattern,
            SbPattern.winterWavePattern,
            SbPattern.winterMagmaLeftPattern,
            SbPattern.winterTotalDmgPattern,
            SbPattern.winterCubeDmgPattern,
            SbPattern.riftDimensionPattern,
            RiftBloodEffigies.heartsPattern,
            SbPattern.wtfAreThoseLinesPattern,
            SbPattern.timeLeftPattern,
            SbPattern.darkAuctionCurrentItemPattern,
            MiningAPI.coldPattern,
            SbPattern.riftHotdogTitlePattern,
            SbPattern.riftHotdogEatenPattern,
            SbPattern.mineshaftNotStartedPattern,
            SbPattern.queuePattern,
            SbPattern.queuePositionPattern,
            SbPattern.fortunateFreezingBonusPattern,
            SbPattern.riftAveikxPattern,
            SbPattern.riftHayEatenPattern,
            SbPattern.fossilDustPattern,
            SbPattern.cluesPattern,
            SbPattern.carnivalPattern,
            SbPattern.carnivalTasksPattern,
            SbPattern.carnivalTokensPattern,
            SbPattern.carnivalFruitsPattern,
            SbPattern.carnivalScorePattern,
            SbPattern.carnivalCatchStreakPattern,
            SbPattern.carnivalAccuracyPattern,
            SbPattern.carnivalKillsPattern,
            *remoteOnlyPatterns,
        )

        unknownLines = unknownLines.filterNot { line ->
            if (line in confirmedLinesCache) return@filterNot true
            val matches = patternsToExclude.any { pattern -> pattern.matches(line) }
            if (matches) confirmedLinesCache += line
            matches
        }


        // Remove known text
        // remove objectives
        val objectiveLine = sidebarLines.firstOrNull { SbPattern.objectivePattern.matches(it) }
            ?: "Objective"
        unknownLines = unknownLines.filter { sidebarLines.nextAfter(objectiveLine) != it }
        // TODO create function
        unknownLines = unknownLines.filter {
            sidebarLines.nextAfter(objectiveLine, 2) != it &&
                !SbPattern.thirdObjectiveLinePattern.matches(it)
        }

        // Remove jacobs contest
        for (i in 1..3) {
            unknownLines = unknownLines.filter {
                sidebarLines.nextAfter(
                    sidebarLines.firstOrNull { line ->
                        SbPattern.jacobsContestPattern.matches(line)
                    } ?: "§eJacob's Contest",
                    i,
                ) != it
            }
        }

        // Remove slayer
        for (i in 1..2) {
            unknownLines = unknownLines.filter {
                sidebarLines.nextAfter(
                    sidebarLines.firstOrNull { line ->
                        SbPattern.slayerQuestPattern.matches(line)
                    } ?: "Slayer Quest",
                    i,
                ) != it
            }
        }

        // remove trapper mob location
        unknownLines = unknownLines.filter {
            sidebarLines.nextAfter(
                sidebarLines.firstOrNull { line ->
                    SbPattern.mobLocationPattern.matches(line)
                } ?: "Tracker Mob Location:",
            ) != it
        }

        // da
        unknownLines = unknownLines.filter {
            sidebarLines.nextAfter(
                sidebarLines.firstOrNull { line ->
                    SbPattern.darkAuctionCurrentItemPattern.matches(line)
                } ?: "Current Item:",
            ) != it
        }

        /*
         * Handle broken scoreboard lines
         */
        confirmedUnknownLines = confirmedUnknownLines.filter { it in unknownLines }

        unknownLines = unknownLines.filter { it !in confirmedUnknownLines }

        unconfirmedUnknownLines = unknownLines

        unknownLines = unknownLines.filter { it !in unknownLinesSet }

        unknownLines.forEach {
            ChatUtils.debug("Unknown Scoreboard line: '$it'")
            unknownLinesSet.add(it)
        }
    }
}
