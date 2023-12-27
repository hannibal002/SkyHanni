package at.hannibal2.skyhanni.features.misc.customscoreboard

import at.hannibal2.skyhanni.data.BitsAPI
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.rift.area.stillgorechateau.RiftBloodEffigies
import at.hannibal2.skyhanni.utils.LorenzUtils.nextAfter
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.StringUtils.trimWhiteSpaceAndResets
import at.hannibal2.skyhanni.utils.TabListData

object InformationGetter {
    fun getInformation() {
        val sidebarLines = ScoreboardData.sidebarLinesFormatted

        for (quirkyLine in TabListData.getTabList()) {
            val line = quirkyLine.trimWhiteSpaceAndResets().removeResets()

            ScoreboardPattern.gemsPattern.matchMatcher(line) {
                gems = group("gems")
            }
            ScoreboardPattern.bankPattern.matchMatcher(line) {
                bank = group("bank")
            }
        }

        // I know this could maybe be solved better but honestly idc anymore
        val knownLines = listOf(
            /*"Instance Shutdow",
            "§f§lWave: §c§l",
            "§fTokens: ",
            "Submerges In: §e",
            "§fObjective",*/
            "Objective",
            "Event Start: §a",
            "Next Wave: §a",
            "§cWave",
            "Magma Cubes Left",
            "Your Total Damag",
            "Your Cube Damage",
            "§6Spooky Festival§f",
            "§dNew Year Event",
            "Nearby Players:",
            "Event: ",
            "Zone: ",
            "§4Broodmother§7:",
            "§7Give Tasty Mithril to Don!",
            "Remaining: §a",
            "Your Tasty Mithr",
            "Points: ",
            "Challenge:",
            "Pelts: §5",
            "Tracker Mob Location:",
            "Time Left: §b",
            "Current Item:",
            "Find tickets on the",
            "ground and bring them",
            "to the raffle box",
            "Tickets: §a",
            "Pool: §6"
        )

        unknownLines = sidebarLines.toMutableList().filter { it.isNotBlank() }.map { it.removeResets() }

        /*
         * remove with pattern
        */
        unknownLines = unknownLines.filter { !ScoreboardPattern.pursePattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.motesPattern.matches(it) }
        unknownLines = unknownLines.filter { !BitsAPI.bitsScoreboardPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.heatPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.copperPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.locationPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.lobbyCodePattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.datePattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.timePattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.footerPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.yearVotesPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.votesPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.waitingForVotePattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.northstarsPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.profileTypePattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.autoClosingPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.startingInPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.timeElapsedPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.keysPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.clearedPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.soloPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.teammatesPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.medalsPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.lockedPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.cleanUpPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.pastingPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.powderPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.windCompassPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.windCompassArrowPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.magmaBossPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.damageSoakedPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.damagedSoakedBarPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.killMagmasPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.killMagmasBarPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.reformingPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.bossHealthPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.bossHealthBarPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.broodmotherPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.bossHPPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.bossDamagePattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.essencePattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.brokenRedstonePattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.visitingPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.flightDurationPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.travelingZooPattern.matches(it) }
        unknownLines = unknownLines.filter { !ScoreboardPattern.riftDimensionPattern.matches(it) }
        unknownLines = unknownLines.filter { !RiftBloodEffigies.heartsPattern.matches(it) }


        /*
         * remove known text
        */
        unknownLines = unknownLines.filter { line -> !knownLines.any { line.trim().contains(it) } }

        // remove objectives kuudra
        unknownLines = unknownLines.filter { sidebarLines.nextAfter("§fObjective") != it }
        unknownLines = unknownLines.filter {
            sidebarLines.nextAfter("§fObjective", 2) != it && !extraObjectiveKuudraLines.contains(it)
        }

        // remove objectives
        val objectiveLine =
            ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("Objective") } ?: "Objective"
        unknownLines = unknownLines.filter { sidebarLines.nextAfter(objectiveLine) != it }
        unknownLines =
            unknownLines.filter { sidebarLines.nextAfter(objectiveLine, 2) != it && !extraObjectiveLines.contains(it) }

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
