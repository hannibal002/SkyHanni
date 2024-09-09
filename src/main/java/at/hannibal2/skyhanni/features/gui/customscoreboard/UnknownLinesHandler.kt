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

internal var confirmedLinesCache = SizeLimitedSet<String>(100)

internal var confirmedUnknownLines = listOf<String>()
internal var unconfirmedUnknownLines = listOf<String>()

internal var pastUnknownLines = SizeLimitedSet<String>(100)

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
        "Previous Unknown Lines" to pastUnknownLines.toSet(),
        noStackTrace = true,
        betaOnly = true,
    )
}

private class UndetectedScoreboardLines(message: String) : Exception(message)

object UnknownLinesHandler {

    internal lateinit var remoteOnlyPatterns: Array<Pattern>

    fun handleUnknownLines() {
        with(ScoreboardPattern) {
            val sidebarLines = CustomScoreboard.activeLines

            var unknownLines = sidebarLines.map { it.removeResets() }.filter { it.isNotBlank() || it.trim().length > 3 }

            /**
             * Remove known lines with patterns
             **/
            val patternsToExclude = mutableListOf(
                PurseAPI.coinsPattern,
                motesPattern,
                BitsAPI.bitsScoreboardPattern,
                heatPattern,
                copperPattern,
                locationPattern,
                lobbyCodePattern,
                datePattern,
                timePattern,
                footerPattern,
                yearVotesPattern,
                votesPattern,
                waitingForVotePattern,
                northstarsPattern,
                profileTypePattern,
                autoClosingPattern,
                startingInPattern,
                timeElapsedPattern,
                instanceShutdownPattern,
                keysPattern,
                clearedPattern,
                soloPattern,
                teammatesPattern,
                floor3GuardiansPattern,
                m7dragonsPattern,
                wavePattern,
                tokensPattern,
                submergesPattern,
                medalsPattern,
                lockedPattern,
                cleanUpPattern,
                pastingPattern,
                peltsPattern,
                mobLocationPattern,
                jacobsContestPattern,
                plotPattern,
                powderGreedyPattern,
                windCompassPattern,
                windCompassArrowPattern,
                miningEventPattern,
                miningEventZonePattern,
                raffleUselessPattern,
                raffleTicketsPattern,
                rafflePoolPattern,
                mithrilUselessPattern,
                mithrilRemainingPattern,
                mithrilYourMithrilPattern,
                nearbyPlayersPattern,
                uselessGoblinPattern,
                remainingGoblinPattern,
                yourGoblinKillsPattern,
                magmaBossPattern,
                damageSoakedPattern,
                killMagmasPattern,
                killMagmasDamagedSoakedBarPattern,
                reformingPattern,
                bossHealthPattern,
                bossHealthBarPattern,
                broodmotherPattern,
                bossHPPattern,
                bossDamagePattern,
                slayerQuestPattern,
                essencePattern,
                redstonePattern,
                anniversaryPattern,
                visitingPattern,
                flightDurationPattern,
                dojoChallengePattern,
                dojoDifficultyPattern,
                dojoPointsPattern,
                dojoTimePattern,
                objectivePattern,
                ServerRestartTitle.restartingGreedyPattern,
                travelingZooPattern,
                newYearPattern,
                spookyPattern,
                winterEventStartPattern,
                winterNextWavePattern,
                winterWavePattern,
                winterMagmaLeftPattern,
                winterTotalDmgPattern,
                winterCubeDmgPattern,
                riftDimensionPattern,
                RiftBloodEffigies.heartsPattern,
                wtfAreThoseLinesPattern,
                timeLeftPattern,
                darkAuctionCurrentItemPattern,
                MiningAPI.coldPattern,
                riftHotdogTitlePattern,
                riftHotdogEatenPattern,
                mineshaftNotStartedPattern,
                queuePattern,
                queueTierPattern,
                queuePositionPattern,
                fortunateFreezingBonusPattern,
                riftAveikxPattern,
                riftHayEatenPattern,
                fossilDustPattern,
                cluesPattern,
                barryProtestorsQuestlinePattern,
                barryProtestorsHandledPattern,
                carnivalPattern,
                carnivalTasksPattern,
                carnivalTokensPattern,
                carnivalFruitsPattern,
                carnivalScorePattern,
                carnivalCatchStreakPattern,
                carnivalAccuracyPattern,
                carnivalKillsPattern,
            )

            if (::remoteOnlyPatterns.isInitialized) {
                patternsToExclude.addAll(remoteOnlyPatterns)
            }

            unknownLines = unknownLines.filterNot { line ->
                if (line in confirmedLinesCache) return@filterNot true
                val matches = patternsToExclude.any { pattern -> pattern.matches(line) }
                if (matches) confirmedLinesCache += line
                matches
            }

            /**
             * Remove Known Text
             **/
            // Remove objectives
            val objectiveLine = sidebarLines.firstOrNull { objectivePattern.matches(it) } ?: "Objective"

            unknownLines = unknownLines.filter { line ->
                val nextLine = sidebarLines.nextAfter(objectiveLine)
                val secondNextLine = sidebarLines.nextAfter(objectiveLine, 2)
                val thirdNextLine = sidebarLines.nextAfter(objectiveLine, 3)

                line != nextLine && line != secondNextLine && line != thirdNextLine && !thirdObjectiveLinePattern.matches(line)
            }

            // Remove jacobs contest
            for (i in 1..3) {
                unknownLines = unknownLines.filter {
                    sidebarLines.nextAfter(
                        sidebarLines.firstOrNull { line ->
                            jacobsContestPattern.matches(line)
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
                            slayerQuestPattern.matches(line)
                        } ?: "Slayer Quest",
                        i,
                    ) != it
                }
            }

            // remove trapper mob location
            unknownLines = unknownLines.filter {
                sidebarLines.nextAfter(
                    sidebarLines.firstOrNull { line ->
                        mobLocationPattern.matches(line)
                    } ?: "Tracker Mob Location:",
                ) != it
            }

            // da
            unknownLines = unknownLines.filter {
                sidebarLines.nextAfter(
                    sidebarLines.firstOrNull { line ->
                        darkAuctionCurrentItemPattern.matches(line)
                    } ?: "Current Item:",
                ) != it
            }

            /**
             * Handle broken scoreboard lines
             */
            confirmedUnknownLines = confirmedUnknownLines.filter { it in unknownLines }

            unknownLines = unknownLines.filter { it !in confirmedUnknownLines }

            unconfirmedUnknownLines = unknownLines

            unknownLines = unknownLines.filter { it !in unknownLinesSet }

            unknownLines.forEach {
                pastUnknownLines += it
                if (LorenzUtils.inSkyBlock) {
                    ChatUtils.debug("Unknown Scoreboard line: '$it'")
                }
                unknownLinesSet.add(it)
            }
    }
}
