package at.hannibal2.skyhanni.features.misc.customscoreboard

import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.LorenzUtils.nextAfter
import at.hannibal2.skyhanni.utils.TabListData
import net.minecraft.scoreboard.Score
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class InformationGetter {
    fun getInformation() {
        // Gets some values from the tablist
        for (line in TabListData.getTabList()) {
            when {
                line.startsWith(" Gems: §r§a") -> gems = line.removePrefix(" Gems: §r§a")
                line.startsWith(" Bank: §r§6") -> bank = line.removePrefix(" Bank: §r§6")
                line.startsWith(" §r§fMithril Powder: §r§2") -> mithrilPowder =
                    line.removePrefix(" §r§fMithril Powder: §r§2")

                line.startsWith(" §r§fGemstone Powder: §r§d") -> gemstonePowder =
                    line.removePrefix(" §r§fGemstone Powder: §r§d")
            }
        }

        // Gets some values from the scoreboard
        for (line in ScoreboardData.sidebarLinesFormatted) {
            when {
                line.startsWith(" §7⏣ ") || line.startsWith(" §5ф ") -> location = line
                line.startsWith("Purse: §6") || line.startsWith("Piggy: §6") -> purse =
                    line.removePrefix("Purse: §6").removePrefix("Piggy: §6")

                line.startsWith("Motes: §d") -> motes = line.removePrefix("Motes: §d")
                extractLobbyCode(line) is String -> lobbyCode =
                    extractLobbyCode(line)?.substring(1) ?: "<hidden>" //removes first char (number of color code)
                line.startsWith("Heat: ") -> heat = line.removePrefix("Heat: ")
                line.startsWith("Bits: §b") -> bits = line.removePrefix("Bits: §b")
                line.startsWith("Copper: §c") -> copper = line.removePrefix("Copper: §c")
            }
        }
    }

    @SubscribeEvent
    fun onProfileSwitch(event: ProfileJoinEvent) {
        // Reset Bits - We need this bc if another profile has 0 bits, it won't show the bits line
        bits = "0"
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        val knownLines = listOf(
            "§7⏣ ",
            "§5ф ",
            "Purse: §6",
            "Piggy: §6",
            "Motes: §d",
            "Heat: ",
            "Bits: §b",
            "Copper: §c",
            lobbyCode ?: "",
            "§ewww.hyp",
            "§ealpha.hyp",
            "§cServer closing: ",
            "Auto-closing in:",
            "Starting in:",
            "Keys: ",
            "Time Elapsed:",
            "§rCleared: ",
            "Instance ShutdowIn:",
            "Time Elapsed: ",
            "§f§lWave: §c§l",
            "§fTokens: ",
            "Submerges In: §e",
            "§fObjective:",
            "Objective:",
            "§eJacob's Contest",
            "§6§lGOLD §fmedals",
            "§f§lSILVER §fmedals",
            "§c§lBRONZE §fmedals",
            "North Stars: §d",
            "Event Start: §a",
            "Next Wave: §a",
            "§cWave",
            "Magma Cubes Left§c",
            "Your Total Damag §c",
            "Your Cube Damage§c",
            "§6Spooky Festival§f",
            "§dNew Year Event",
            "§aTraveling Zoo",
            "§9Wind Compass",
            "Nearby Players:",
            "Event: ",
            "Zone: ",
            "Protector HP: §a",
            "Dragon HP: §a",
            "Your Damage: §c",
            "Essence: "
            )

        extraLines = ScoreboardData.sidebarLinesFormatted.filter { line -> !knownLines.any { line.startsWith(it) } }

        // filter empty lines
        extraLines.filter { it.trim() == "" }

        // remove objectives
        extraLines.filter { ScoreboardData.sidebarLinesFormatted.nextAfter("§fObjective:") == it }
        extraLines.filter { ScoreboardData.sidebarLinesFormatted.nextAfter("Objective:") == it }

        // remove wind compass
        extraLines.filter { ScoreboardData.sidebarLinesFormatted.nextAfter("§9Wind Compass") == it }

        // Remove dungeon teammates
        val dungeonPlayers = TabListData.getTabList().firstOrNull { it.trim().startsWith("§r§b§lParty §r§f(") }
            ?.trim()?.removePrefix("§r§b§lParty §r§f(")?.removeSuffix(")")?.toInt() ?: 1
        val clearedLine = ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("§rCleared: ") }.toString()

        if (dungeonPlayers != 0) {
            if (dungeonPlayers > 1) {
                for (i in 1..dungeonPlayers) {
                    extraLines.filter { ScoreboardData.sidebarLinesFormatted.nextAfter(clearedLine, i) == it }
                }
            }
        }

        // Remove jacobs contest
        for (i in 1..3)
        extraLines.filter { ScoreboardData.sidebarLinesFormatted.nextAfter("§eJacob's Contest", i) == it }
    }
}
