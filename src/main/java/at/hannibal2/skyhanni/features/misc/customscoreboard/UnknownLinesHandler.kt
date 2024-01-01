package at.hannibal2.skyhanni.features.misc.customscoreboard

import at.hannibal2.skyhanni.data.BitsAPI
import at.hannibal2.skyhanni.data.PurseAPI
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.misc.ServerRestartTitle
import at.hannibal2.skyhanni.features.rift.area.stillgorechateau.RiftBloodEffigies
import at.hannibal2.skyhanni.utils.LorenzUtils.nextAfter
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeResets

object UnknownLinesHandler {
    fun handleUnknownLines() {
        val sidebarLines = ScoreboardData.sidebarLinesFormatted

        val knownLines = listOf(
            "Time Left: §b",
            "Current Item:",
        )

        /**
        * Dark auction will come once DarkAuction IslandType is merged
        *
        *    Starting in: §a0:02
        *
        *    'Time Left: §b11'
        *    'Current Item:'
        *    ' §5Hocus-Pocus Cipher'
        */

        unknownLines = sidebarLines.toMutableList().filter { it.isNotBlank() }.map { it.removeResets() }

        /*
         * remove with pattern
        */
        val patternsToExclude = listOf(
            PurseAPI.pursePattern,
            ScoreboardPattern.motesPattern,
            BitsAPI.bitsScoreboardPattern,
            ScoreboardPattern.heatPattern,
            ScoreboardPattern.copperPattern,
            ScoreboardPattern.locationPattern,
            ScoreboardPattern.lobbyCodePattern,
            ScoreboardPattern.datePattern,
            ScoreboardPattern.timePattern,
            ScoreboardPattern.footerPattern,
            ScoreboardPattern.yearVotesPattern,
            ScoreboardPattern.votesPattern,
            ScoreboardPattern.waitingForVotePattern,
            ScoreboardPattern.northstarsPattern,
            ScoreboardPattern.profileTypePattern,
            ScoreboardPattern.autoClosingPattern,
            ScoreboardPattern.startingInPattern,
            ScoreboardPattern.timeElapsedPattern,
            ScoreboardPattern.keysPattern,
            ScoreboardPattern.clearedPattern,
            ScoreboardPattern.soloPattern,
            ScoreboardPattern.teammatesPattern,
            ScoreboardPattern.floor3GuardiansPattern,
            ScoreboardPattern.medalsPattern,
            ScoreboardPattern.lockedPattern,
            ScoreboardPattern.cleanUpPattern,
            ScoreboardPattern.pastingPattern,
            ScoreboardPattern.peltsPattern,
            ScoreboardPattern.mobLocationPattern,
            ScoreboardPattern.jacobsContestPattern,
            ScoreboardPattern.powderPattern,
            ScoreboardPattern.windCompassPattern,
            ScoreboardPattern.windCompassArrowPattern,
            ScoreboardPattern.miningEventPattern,
            ScoreboardPattern.miningEventZonePattern,
            ScoreboardPattern.raffleUselessPattern,
            ScoreboardPattern.raffleTicketsPattern,
            ScoreboardPattern.rafflePoolPattern,
            ScoreboardPattern.mithrilUselessPattern,
            ScoreboardPattern.mithrilRemainingPattern,
            ScoreboardPattern.mithrilYourMithrilPattern,
            ScoreboardPattern.nearbyPlayersPattern,
            ScoreboardPattern.uselessGoblinPattern,
            ScoreboardPattern.remainingGoblinPattern,
            ScoreboardPattern.yourGoblinKillsPattern,
            ScoreboardPattern.magmaBossPattern,
            ScoreboardPattern.damageSoakedPattern,
            ScoreboardPattern.damagedSoakedBarPattern,
            ScoreboardPattern.killMagmasPattern,
            ScoreboardPattern.killMagmasBarPattern,
            ScoreboardPattern.reformingPattern,
            ScoreboardPattern.bossHealthPattern,
            ScoreboardPattern.bossHealthBarPattern,
            ScoreboardPattern.broodmotherPattern,
            ScoreboardPattern.bossHPPattern,
            ScoreboardPattern.bossDamagePattern,
            ScoreboardPattern.slayerQuestPattern,
            ScoreboardPattern.essencePattern,
            ScoreboardPattern.brokenRedstonePattern,
            ScoreboardPattern.redstonePattern,
            ScoreboardPattern.visitingPattern,
            ScoreboardPattern.flightDurationPattern,
            ScoreboardPattern.dojoChallengePattern,
            ScoreboardPattern.dojoDifficultyPattern,
            ScoreboardPattern.dojoPointsPattern,
            ScoreboardPattern.dojoTimePattern,
            ScoreboardPattern.objectivePattern,
            ServerRestartTitle.restartingPattern,
            ScoreboardPattern.travelingZooPattern,
            ScoreboardPattern.newYearPattern,
            ScoreboardPattern.spookyPattern,
            ScoreboardPattern.winterEventStartPattern,
            ScoreboardPattern.winterNextWavePattern,
            ScoreboardPattern.winterWavePattern,
            ScoreboardPattern.winterMagmaLeftPattern,
            ScoreboardPattern.winterTotalDmgPattern,
            ScoreboardPattern.winterCubeDmgPattern,
            ScoreboardPattern.riftDimensionPattern,
            RiftBloodEffigies.heartsPattern
        )

        unknownLines = unknownLines.filterNot { line ->
            patternsToExclude.any { pattern -> pattern.matches(line) }
        }


        /*
         * remove known text
        */
        unknownLines = unknownLines.filter { line -> !knownLines.any { line.trim().contains(it) } }

        // remove objectives
        val objectiveLine =
            ScoreboardData.sidebarLinesFormatted.firstOrNull { ScoreboardPattern.objectivePattern.matches(it) }
                ?: "Objective"
        unknownLines = unknownLines.filter { sidebarLines.nextAfter(objectiveLine) != it }
        unknownLines = unknownLines.filter {
                sidebarLines.nextAfter(objectiveLine, 2) != it
                    && !ScoreboardPattern.thirdObjectiveLinePattern.matches(it)
            }

        // Remove jacobs contest
        for (i in 1..3)
            unknownLines = unknownLines.filter {
                ScoreboardData.sidebarLinesFormatted.nextAfter(ScoreboardData.sidebarLinesFormatted.firstOrNull { line ->
                    ScoreboardPattern.jacobsContestPattern.matches(line)
                } ?: "§eJacob's Contest", i) != it
            }

        // Remove slayer
        for (i in 1..2)
            unknownLines = unknownLines.filter {
                ScoreboardData.sidebarLinesFormatted.nextAfter(ScoreboardData.sidebarLinesFormatted.firstOrNull { line ->
                    ScoreboardPattern.slayerQuestPattern.matches(line)
                } ?: "Slayer Quest", i) != it
            }

        // remove trapper mob location
        unknownLines = unknownLines.filter {
            ScoreboardData.sidebarLinesFormatted.nextAfter(ScoreboardData.sidebarLinesFormatted.firstOrNull { line ->
                ScoreboardPattern.mobLocationPattern.matches(line)
            } ?: "Tracker Mob Location:", 1) != it
        }

        // da
        unknownLines = unknownLines.filter { sidebarLines.nextAfter("Current Item:") != it }
    }
}
