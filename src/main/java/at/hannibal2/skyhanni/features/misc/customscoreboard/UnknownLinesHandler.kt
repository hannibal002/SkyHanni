package at.hannibal2.skyhanni.features.misc.customscoreboard

import at.hannibal2.skyhanni.data.BitsAPI
import at.hannibal2.skyhanni.data.PurseAPI
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.rift.area.stillgorechateau.RiftBloodEffigies
import at.hannibal2.skyhanni.utils.LorenzUtils.nextAfter
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeResets

object UnknownLinesHandler {
    fun handleUnknownLines() {
        val sidebarLines = ScoreboardData.sidebarLinesFormatted

        // I know this could maybe be solved better but honestly idc anymore
        // - Update I care or rather hanni wants me to do that idk I dont want to get removed as Contrib
        val knownLines = listOf(
            /*"Instance Shutdow",   leaving them like this because I want people to report them so I can get the
            "§f§lWave: §c§l",       exact lines (i really dont want to run kuudra myself)
            "§fTokens: ",
            "Submerges In: §e"*/
            "Event Start: §a",
            "Next Wave: §a",
            "§cWave",
            "Magma Cubes Left",
            "Your Total Damag",
            "Your Cube Damage",
            "§6Spooky Festival§f",
            "§dNew Year Event",
            "Pelts: §5",
            "Tracker Mob Location:",
            "Time Left: §b",
            "Current Item:",
        )

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
            ScoreboardPattern.medalsPattern,
            ScoreboardPattern.lockedPattern,
            ScoreboardPattern.cleanUpPattern,
            ScoreboardPattern.pastingPattern,
            ScoreboardPattern.powderPattern,
            ScoreboardPattern.windCompassPattern,
            ScoreboardPattern.windCompassArrowPattern,
            ScoreboardPattern.miningEventPattern,
            ScoreboardPattern.miningEventZonePattern,
            ScoreboardPattern.raffleUselessPattern,
            ScoreboardPattern.raffleTicketsPattern,
            ScoreboardPattern.rafflePool,
            ScoreboardPattern.mithrilUselessPattern,
            ScoreboardPattern.mithrilRemainingPattern,
            ScoreboardPattern.mithrilYourMithrilPattern,
            ScoreboardPattern.nearbyPlayersPattern,
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
            ScoreboardPattern.essencePattern,
            ScoreboardPattern.brokenRedstonePattern,
            ScoreboardPattern.visitingPattern,
            ScoreboardPattern.flightDurationPattern,
            ScoreboardPattern.dojoChallengePattern,
            ScoreboardPattern.dojoDifficultyPattern,
            ScoreboardPattern.dojoPointsPattern,
            ScoreboardPattern.dojoTimePattern,
            ScoreboardPattern.objectivePattern,
            ScoreboardPattern.travelingZooPattern,
            ScoreboardPattern.riftDimensionPattern,
            RiftBloodEffigies.heartsPattern
        )

        unknownLines = unknownLines.filter { line ->
            patternsToExclude.none { pattern -> pattern.matches(line) }
        }


        /*
         * remove known text
        */
        unknownLines = unknownLines.filter { line -> !knownLines.any { line.trim().contains(it) } }

        // remove objectives
        val objectiveLine =
            ScoreboardData.sidebarLinesFormatted.first { ScoreboardPattern.objectivePattern.matches(it) }
        unknownLines =
            unknownLines.filter {
                sidebarLines.nextAfter(objectiveLine, 2) != it
                    && (!extraObjectiveLines.contains(it)
                    || !extraObjectiveKuudraLines.contains(it))
            }

        // Remove jacobs contest
        for (i in 0..3)
            unknownLines = unknownLines.filter { sidebarLines.nextAfter("§eJacob's Contest", i) != it }

        // Remove slayer
        for (i in 0..2)
            unknownLines = unknownLines.filter { sidebarLines.nextAfter("Slayer Quest", i) != it }

        // remove trapper mob location
        unknownLines = unknownLines.filter { sidebarLines.nextAfter("Tracker Mob Location:", 1) != it }

        // da
        unknownLines = unknownLines.filter { sidebarLines.nextAfter("Current Item:") != it }
    }
}
