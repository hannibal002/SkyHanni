package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.data.BitsAPI
import at.hannibal2.skyhanni.data.PurseAPI
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.misc.ServerRestartTitle
import at.hannibal2.skyhanni.features.rift.area.stillgorechateau.RiftBloodEffigies
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.nextAfter
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern as SbPattern

object UnknownLinesHandler {
    fun handleUnknownLines() {
        val sidebarLines = ScoreboardData.sidebarLinesFormatted

        unconfirmedUnknownLines = sidebarLines
            .map { it.removeResets() }
            .filter { it.isNotBlank() }
            .filter { it.trim().length > 3 }

        /*
         * Remove known lines with patterns
        */
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
            SbPattern.powderPattern,
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
            SbPattern.visitingPattern,
            SbPattern.flightDurationPattern,
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
            SbPattern.coldPattern,
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
        )

        unconfirmedUnknownLines = unconfirmedUnknownLines.filterNot { line ->
            patternsToExclude.any { pattern -> pattern.matches(line) }
        }

        /*
         * remove known text
        */
        // remove objectives
        val objectiveLine =
            sidebarLines.firstOrNull { SbPattern.objectivePattern.matches(it) }
                ?: "Objective"
        unconfirmedUnknownLines = unconfirmedUnknownLines.filter { sidebarLines.nextAfter(objectiveLine) != it }
        // TODO create function
        unconfirmedUnknownLines = unconfirmedUnknownLines.filter {
            sidebarLines.nextAfter(objectiveLine, 2) != it
                && !SbPattern.thirdObjectiveLinePattern.matches(it)
        }

        // Remove jacobs contest
        for (i in 1..3)
            unconfirmedUnknownLines = unconfirmedUnknownLines.filter {
                sidebarLines.nextAfter(sidebarLines.firstOrNull { line ->
                    SbPattern.jacobsContestPattern.matches(line)
                } ?: "§eJacob's Contest", i) != it
            }

        // Remove slayer
        for (i in 1..2)
            unconfirmedUnknownLines = unconfirmedUnknownLines.filter {
                sidebarLines.nextAfter(sidebarLines.firstOrNull { line ->
                    SbPattern.slayerQuestPattern.matches(line)
                } ?: "Slayer Quest", i) != it
            }

        // remove trapper mob location
        unconfirmedUnknownLines = unconfirmedUnknownLines.filter {
            sidebarLines.nextAfter(sidebarLines.firstOrNull { line ->
                SbPattern.mobLocationPattern.matches(line)
            } ?: "Tracker Mob Location:") != it
        }

        // da
        unconfirmedUnknownLines = unconfirmedUnknownLines.filter {
            sidebarLines.nextAfter(sidebarLines.firstOrNull { line ->
                SbPattern.darkAuctionCurrentItemPattern.matches(line)
            } ?: "Current Item:") != it
        }


        /*
         * handle broken scoreboard lines
         */
        confirmedUnknownLines.forEach { line ->
            if (!unconfirmedUnknownLines.contains(line)) {
                confirmedUnknownLines = confirmedUnknownLines.filterNot { it == line }.toMutableList()
                unconfirmedUnknownLines = unconfirmedUnknownLines.filterNot { it == line }
            }
        }
        unconfirmedUnknownLines.forEach { line ->
            if (confirmedUnknownLines.contains(line)) {
                unconfirmedUnknownLines = unconfirmedUnknownLines.filterNot { it == line }
            } else if (!unknownLinesSet.contains(line)) {
                ChatUtils.debug("Unknown Scoreboard line: '$line'")
                unknownLinesSet.add(line)
            }
        }
    }
}
