package at.hannibal2.skyhanni.features.misc.customscoreboard

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.utils.LorenzUtils.inDungeons
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.LorenzUtils.nextAfter
import at.hannibal2.skyhanni.utils.TabListData
import java.util.function.Supplier

private val config get() = SkyHanniMod.feature.gui.customScoreboard

enum class Events(private val displayLine: Supplier<List<String>>, private val showWhen: () -> Boolean) {
    VOTING(
        {
            val list = mutableListOf<String>()

            list += ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("§6Year ") && it.endsWith(" Votes") } ?: "<hidden>"
            if (ScoreboardData.sidebarLinesFormatted.nextAfter(list[0]) == "§7Waiting for") {
                list += "§7Waiting for"
                list += "§7your vote..."
            } else {
                for (i in 1 until 6) {
                    list += ScoreboardData.sidebarLinesFormatted.nextAfter(list[0], i) ?: "<hidden>"
                }
            }

            list
        },
        {
            ScoreboardData.sidebarLinesFormatted.any { it.startsWith("§6Year ") && it.endsWith(" Votes") }
        }
    ),
    SERVER_CLOSE(
        {
            listOf(ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("§cServer closing: ") }
                ?: "<hidden>")
        },
        {
            ScoreboardData.sidebarLinesFormatted.any { it.startsWith("§cServer closing: ") }
        }
    ),
    DUNGEONS(
        {
            val list = mutableListOf<String>()

            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Auto-closing in:") }) {
                list += ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("Auto-closing in:") }
                    ?: "<hidden>"
            }
            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Starting in:") }) {
                list += ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("Starting in:") } ?: "<hidden>"
            }

            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Keys: ") }) {
                list += ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("Keys: ") } ?: "<hidden>"
            }
            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Time Elapsed: ") }) {
                list += ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("Time Elapsed: ") }
                    ?: "<hidden>"
            }
            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("§rCleared: ") || it.startsWith("Cleared: ") }) {
                list += ScoreboardData.sidebarLinesFormatted.firstOrNull {
                    it.startsWith("§rCleared: ") || it.startsWith(
                        "Cleared:"
                    )
                }.toString().replace("§r", "")
            }

            if (ScoreboardData.sidebarLinesFormatted.any { lines ->
                    InformationGetter.dungeonClassList.any {
                        lines.startsWith(it)
                    }
                }) {
                list += ScoreboardData.sidebarLinesFormatted.filter {
                    InformationGetter.dungeonClassList.any { lines ->
                        it.startsWith(lines)
                    }
                }.map { it.replace("[Lv", "§7[Lv") }
            }

            if (list.size == 0) when (config.informationFilteringConfig.hideEmptyLines) {
                true -> listOf("<hidden>")
                false -> listOf("§cNo Dungeon Data")
            } else list
        },
        {
            IslandType.CATACOMBS.isInIsland() || inDungeons
        }
    ),
    KUUDRA(
        {
            val list = mutableListOf<String>()

            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Auto-closing in:") }) {
                ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("Auto-closing in:") }
            }
            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Starting in:") }) {
                list += ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("Starting in:") } ?: "<hidden>"
            }
            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Instance Shutdow") }) {
                list += ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("Instance Shutdow") }
                    ?: "<hidden>"
            }

            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Time Elapsed: ") }) {
                list += ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("Time Elapsed: ") }
                    ?: "<hidden>"
            }
            list += ""
            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("§f§lWave: §c§l") }) {
                list += ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("§f§lWave: §c§l") }
                    ?: "<hidden>"
            }
            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("§fTokens: ") }) {
                list += ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("§fTokens: ") } ?: "<hidden>"
            }
            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Submerges In: §e") }) {
                list += ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("Submerges In: §e") }
                    ?: "<hidden>"
            }
            list += ""
            if (ScoreboardData.sidebarLinesFormatted.any { it == "§fObjective:" }) {
                list += "§fObjective:"
                list += ScoreboardData.sidebarLinesFormatted.nextAfter("§fObjective:") ?: "§cNo Objective"
            }

            if (list.size == 0) when (config.informationFilteringConfig.hideEmptyLines) {
                true -> listOf("<hidden>")
                false -> listOf("§cNo Kuudra Data")
            } else list
        },
        {
            IslandType.KUUDRA_ARENA.isInIsland()
        }
    ),
    DOJO(
        {
            val list = mutableListOf<String>()

            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Challenge: ") }) {
                list += ScoreboardData.sidebarLinesFormatted.first { it.startsWith("Challenge: ") }
            }

            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Points: ") }) {
                list += ScoreboardData.sidebarLinesFormatted.first { it.startsWith("Points: ") }
            }

            if (list.size == 0) when (config.informationFilteringConfig.hideEmptyLines) {
                true -> listOf("<hidden>")
                false -> listOf("§cNo Dojo Data")
            } else list
        },
        {
            ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Challenge: ") }
        }
    ),
    MAGMA_BOSS(
        {
            val list = mutableListOf<String>()

            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("§7Boss: §c")}){
                list += ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("§7Boss: §c") }
                    ?: "<hidden>"
            }

            if (ScoreboardData.sidebarLinesFormatted.any { it == "§7Damage Soaked:"}) {
                list += "§7Damage Soaked:"
                list += ScoreboardData.sidebarLinesFormatted.nextAfter("§7Damage Soaked:") ?: "<hidden>"
            }

            if (ScoreboardData.sidebarLinesFormatted.any { it == "§6Kill the Magmas:"}){
                list += "§6Kill the Magmas:"
                list += ScoreboardData.sidebarLinesFormatted.nextAfter("§6Kill the Magmas:") ?: "<hidden>"
            }

            list
        },
        {
            ScoreboardData.sidebarLinesFormatted.any { it.startsWith("§7Boss: §c") || it == "§6Kill the Magmas:" }
        }
    ),
    JACOB_CONTEST(
        {
            val list = mutableListOf<String>()

            list += "§eJacob's Contest"
            list += ScoreboardData.sidebarLinesFormatted.nextAfter("§eJacob's Contest") ?: "§7No Event"
            list += ScoreboardData.sidebarLinesFormatted.nextAfter("§eJacob's Contest", 2) ?: "§7No Ranking"
            list += ScoreboardData.sidebarLinesFormatted.nextAfter("§eJacob's Contest", 3) ?: "§7No Amount for next"

            list
        },
        {
            ScoreboardData.sidebarLinesFormatted.any { it.startsWith("§e○ §f") || it.startsWith("§6☘ §f") }
        }
    ),
    JACOB_MEDALS(
        {
            val list = mutableListOf<String>()

            list += ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("§6§lGOLD §fmedals") }
                ?: "<hidden>"
            list += ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("§f§lSILVER §fmedals") }
                ?: "<hidden>"
            list += ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("§c§lBRONZE §fmedals") }
                ?: "<hidden>"

            list
        },
        {
            ScoreboardData.sidebarLinesFormatted.any { it.startsWith("§6§lGOLD §fmedals") }
        }
    ),
    TRAPPER(
        {
            val list = mutableListOf<String>()

            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Pelts: §5") }) {
                list += ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("Pelts: §5") }
                    ?: "<hidden>"
            }
            if (ScoreboardData.sidebarLinesFormatted.any { it == "Tracker Mob Location:" }) {
                list += "Tracker Mob Location:"
                list += ScoreboardData.sidebarLinesFormatted.nextAfter("Tracker Mob Location:") ?: "<hidden>"
            }

            list
        },
        {
            ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Pelts: §5") || it == "Tracker Mob Location:"}
        }
    ),
    GARDEN_CLEAN_UP(
        {
            listOf(ScoreboardData.sidebarLinesFormatted.firstOrNull { it.trim().startsWith("§fCleanup§7:") }
                ?: "<hidden>")
        },
        {
            ScoreboardData.sidebarLinesFormatted.any { it.trim().startsWith("§fCleanup§7:") }
        }
    ),
    FLIGHT_DURATION(
        {
            listOf(
                ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("Flight Duration:") }
                    ?.replace(":a", ":§a")
                    ?: "<hidden>"
            )
        },
        {
            ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Flight Duration:") }
        }
    ),
    WINTER(
        {
            val list = mutableListOf<String>()
            val sidebarLines = ScoreboardData.sidebarLinesFormatted

            if (sidebarLines.any { it.startsWith("North Stars: §d") }) {
                list += sidebarLines.firstOrNull { it.startsWith("North Stars: §d") }
                    ?: "<hidden>"
            }
            if (sidebarLines.any { it.startsWith("Event Start: §a") }) {
                list += sidebarLines.firstOrNull { it.startsWith("Event Start: §a") }
                    ?: "<hidden>"
            }
            if (sidebarLines.any { it.startsWith("Next Wave: §a") } && sidebarLines.none { it == "Next Wave: §aSoon!" }) {
                list += sidebarLines.firstOrNull { it.startsWith("Next Wave: §a") }
                    ?: "<hidden>"
            }
            list += ""
            if (sidebarLines.any { it.startsWith("§cWave ") }) {
                list += sidebarLines.firstOrNull { it.startsWith("§cWave ") } ?: "<hidden>"
            }
            if (sidebarLines.any { it.startsWith("Magma Cubes Left") }) {
                list += sidebarLines.firstOrNull { it.startsWith("Magma Cubes Left") }
                    ?: "<hidden>"
            }
            if (sidebarLines.any { it.startsWith("Your Total Dama") }) {
                list += sidebarLines.firstOrNull { it.startsWith("Your Total Dama") }
                    ?: "<hidden>"
            }
            if (sidebarLines.any { it.startsWith("Your Cube Damage") }) {
                list += sidebarLines.firstOrNull { it.startsWith("Your Cube Damage") }
                    ?: "<hidden>"
            }

            list
        },
        {
            IslandType.WINTER.isInIsland()
        }
    ),
    SPOOKY(
        {
            listOf(ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("§6Spooky Festival§f") }
                ?: "<hidden>") + // Time
                ("§7Your Candy: ") +
                (CustomScoreboardUtils.getTablistFooter().split("\n").firstOrNull { it.startsWith("§7Your Candy:") }
                    ?.removePrefix("§7Your Candy:") ?: "§cCandy not found") // Candy
        },
        {
            ScoreboardData.sidebarLinesFormatted.any { it.startsWith("§6Spooky Festival§f") }
        }
    ),
    MARINA(
        {
            listOf(
                "§bFishing Festival: " + TabListData.getTabList().nextAfter("§e§lEvent: §r§bFishing Festival")
                    ?.removePrefix(" Ends In: ")
            )
        },
        {
            TabListData.getTabList()
                .any { it.startsWith("§e§lEvent: §r§bFishing Festival") } && TabListData.getTabList()
                .nextAfter("§e§lEvent: §r§bFishing Festival")?.startsWith(" Ends In: ") == true
        }
    ),
    BROODMOTHER(
        {
            listOf(ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("§4Broodmother§7:") } ?: "<hidden>")
        },
        {
            ScoreboardData.sidebarLinesFormatted.any { it.startsWith("§4Broodmother§7:") }
        }
    ),
    NEW_YEAR(
        {
            listOf(ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("§dNew Year Event") } ?: "<hidden>")
        },
        {
            ScoreboardData.sidebarLinesFormatted.any { it.startsWith("§dNew Year Event") }
        }
    ),
    ORINGO(
        {
            listOf(ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("§aTraveling Zoo") } ?: "<hidden>")
        },
        {
            ScoreboardData.sidebarLinesFormatted.any { it.startsWith("§aTraveling Zoo") }
        }
    ),
    MINING_EVENTS(
        {
            val list = mutableListOf<String>()

            // Mining Fiesta
            if (TabListData.getTabList().any { it == "§e§lEvent: §r§6Mining Fiesta" }
                && TabListData.getTabList().nextAfter("§e§lEvent: §r§6Mining Fiesta")
                    ?.startsWith(" Ends In:") == true) {
                list += "§6Mining Fiesta: " + TabListData.getTabList().nextAfter("§e§lEvent: §r§6Mining Fiesta")
                    ?.removePrefix(" Ends In: ")
            }

            // Wind
            if (ScoreboardData.sidebarLinesFormatted.any { it == "§9Wind Compass" }) {
                list += "§9Wind Compass"
                list += ScoreboardData.sidebarLinesFormatted.nextAfter("§9Wind Compass")?.replace("a", "§a")
                    ?: "§7No Wind Compass for some reason"
            }

            // Better Together
            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Nearby Players:") }) {
                list += "§9Better Together"
                list += (" " + ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("Nearby Players:") }
                    ?.replace("Nearby Players: ", "Nearby Players: §a"))
            }

            // Zone Events
            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Event: ") }) {
                val fixName = listOf(
                    "GLOBIRAID" to "GOBLIN RAID",
                    "MITHR GOURMAND" to "MITHRIL GOURMAND",
                )
                list += ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("Event: ") }
                    ?.removePrefix("Event: ")?.let { name ->
                        fixName.firstOrNull { it.first == name }?.second ?: name
                    } ?: "<hidden>"
                if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Zone: ") }) {
                    list += "§fin " + (ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("Zone: ") }
                        ?.removePrefix("Zone: ") ?: "<hidden>")
                }
                if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Remaining: §a") }) {
                    list += ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("Remaining: §a") }
                        ?: "<hidden>"
                    list += ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("Your Tasty Mithr: §c") }
                        ?: "<hidden>"
                }
            }

            if (list.size == 0) when (config.informationFilteringConfig.hideEmptyLines) {
                true -> listOf("<hidden>")
                false -> listOf("§cNo Mining Event")
            } else list
        },
        {
            IslandType.DWARVEN_MINES.isInIsland() || IslandType.CRYSTAL_HOLLOWS.isInIsland()
        }
    ),
    DAMAGE(
        {
            listOf(ScoreboardData.sidebarLinesFormatted.firstOrNull {
                it.startsWith("Protector HP: §a") || it.startsWith(
                    "Dragon HP: §a"
                )
            } ?: "<hidden>") +
                (ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("Your Damage: §c") } ?: "<hidden>")
        },
        {
            ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Your Damage: §c") }
        }
    ),
    ESSENCE(
        {
            listOf(ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith("Essence: ") } ?: "<hidden>")
        },
        {
            ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Essence: ") }
        }
    ),
    NONE( // maybe use default state tablist: "Events: smth" idk
        {
            when {
                config.informationFilteringConfig.hideEmptyLines -> listOf("<hidden>")
                else -> listOf("§cNo Event")
            }
        },
        {
            false
        }
    );

    fun getLines(): List<String> {
        return displayLine.get()
    }

    companion object {
        fun getEvent(): List<Events> {
            if (config.displayConfig.showAllActiveEvents) {
                return entries.filter { it.showWhen() }
            }
            return listOf(entries.firstOrNull { it.showWhen() } ?: NONE)
        }
    }
}
