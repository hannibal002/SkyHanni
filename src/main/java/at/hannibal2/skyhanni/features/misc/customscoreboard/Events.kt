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
    ),
    SERVER_CLOSE(
        {
            listOf(ScoreboardData.sidebarLinesFormatted.first { it.startsWith("§cServer closing: ") })
        },
        {
            ScoreboardData.sidebarLinesFormatted.any { it.startsWith("§cServer closing: ") }
        }
    ),
    DUNGEONS(
        {
            val list = mutableListOf<String>()

            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Auto-closing in:") }) {
                list += ScoreboardData.sidebarLinesFormatted.first { it.startsWith("Auto-closing in:") }
            }
            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Starting in:") }) {
                list += ScoreboardData.sidebarLinesFormatted.first { it.startsWith("Starting in:") }
            }

            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Keys: ") }) {
                list += ScoreboardData.sidebarLinesFormatted.first { it.startsWith("Keys: ") }
            }
            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Time Elapsed: ") }) {
                list += ScoreboardData.sidebarLinesFormatted.first { it.startsWith("Time Elapsed: ") }
            }
            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("§rCleared: ") }) {
                list += ScoreboardData.sidebarLinesFormatted.first { it.startsWith("§rCleared: ") }.toString()
                    .replace("§r", "").replace("%", "%§") // for some reason this is broken
            }

            val dungeonPlayers = TabListData.getTabList().first { it.trim().startsWith("§r§b§lParty §r§f(") }
                .trim().removePrefix("§r§b§lParty §r§f(").removeSuffix(")").toInt()

            if (dungeonPlayers != 0 && list.any { it.startsWith("Cleared: ") }) {
                list += ""

                if (dungeonPlayers == 1) {
                    list += "§3§lSolo"
                } else {
                    for (i in 2..dungeonPlayers) {
                        list += ScoreboardData.sidebarLinesFormatted.nextAfter(
                            "§r" + list.first { it.startsWith("Cleared: ") }.replace("%§", "%"),
                            i
                        )
                            ?: "§cNo Player found"
                    }
                }
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
    KUUDRA( // I really need more kuudra scoreboard data, I dont play kuudra
        {
            val list = mutableListOf<String>()

            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Auto-closing in:") }) {
                ScoreboardData.sidebarLinesFormatted.first { it.startsWith("Auto-closing in:") }
            }
            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Starting in:") }) {
                list += ScoreboardData.sidebarLinesFormatted.first { it.startsWith("Starting in:") }
            }
            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Instance ShutdowIn:") }) {
                list += ScoreboardData.sidebarLinesFormatted.first { it.startsWith("Instance ShutdowIn:") }
                    .replace("Instance ShutdowIn:", "Instance Shutdown In:") // for some reason this is broken
            }

            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Time Elapsed: ") }) {
                list += ScoreboardData.sidebarLinesFormatted.first { it.startsWith("Time Elapsed: ") }
            }
            list += ""
            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("§f§lWave: §c§l")}){
                list += ScoreboardData.sidebarLinesFormatted.first { it.startsWith("§f§lWave: §c§l") }
            }
            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("§fTokens: ")}){
                list += ScoreboardData.sidebarLinesFormatted.first { it.startsWith("§fTokens: ") }
            }
            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Submerges In: §e")}){
                list += ScoreboardData.sidebarLinesFormatted.first { it.startsWith("Submerges In: §e") }
            }
            list += ""
            if (ScoreboardData.sidebarLinesFormatted.any { it == "§fObjective:"}){
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

            list += ScoreboardData.sidebarLinesFormatted.first { it.startsWith("§6§lGOLD §fmedals") }
            list += ScoreboardData.sidebarLinesFormatted.first { it.startsWith("§f§lSILVER §fmedals") }
            list += ScoreboardData.sidebarLinesFormatted.first { it.startsWith("§c§lBRONZE §fmedals") }

            list
        },
        {
            ScoreboardData.sidebarLinesFormatted.any { it.startsWith("§6§lGOLD §fmedals") }
        }
    ),
    WINTER( // not tested
        {
            val list = mutableListOf<String>()

            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("North Stars: §d")}){
                list += ScoreboardData.sidebarLinesFormatted.first { it.startsWith("North Stars: §d") }
            }
            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Event Start: §a")}){
                list += ScoreboardData.sidebarLinesFormatted.first { it.startsWith("Event Start: §a") }
            }
            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Next Wave: §a")}){
                list += ScoreboardData.sidebarLinesFormatted.first { it.startsWith("Next Wave: §a") }
            }
            list += ""
            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("§cWave ")}){
                list += ScoreboardData.sidebarLinesFormatted.first { it.startsWith("§cWave ") }
            }
            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Magma Cubes Left§c")}){
                list += ScoreboardData.sidebarLinesFormatted.first { it.startsWith("Magma Cubes Left§c") }
            }
            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Your Total Damag §c")}){
                list += ScoreboardData.sidebarLinesFormatted.first { it.startsWith("Your Total Damag §c") }.replace("Damag", "Damage")
            }
            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Your Cube Damage§c")}){
                list += ScoreboardData.sidebarLinesFormatted.first { it.startsWith("Your Cube Damage§c") }
            }

            list
        },
        {
            IslandType.WINTER.isInIsland()
        }
    ),
    SPOOKY( // not tested
        {
            listOf(ScoreboardData.sidebarLinesFormatted.first { it.startsWith("§6Spooky Festival§f") }) + // Time
                (getTablistFooter().split("\n").first { it.startsWith("§r§r§7Your Candy:") }) // Candy
        },
        {
            ScoreboardData.sidebarLinesFormatted.any { it.startsWith("§6Spooky Festival§f") }
        }
    ),
    MARINA( // not tested, should work
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
    NEW_YEAR(
        {
            listOf(ScoreboardData.sidebarLinesFormatted.first { it.startsWith("§dNew Year Event") })
        },
        {
            ScoreboardData.sidebarLinesFormatted.any { it.startsWith("§dNew Year Event") }
        }
    ),
    ORINGO(
        {
            listOf(ScoreboardData.sidebarLinesFormatted.first { it.startsWith("§aTraveling Zoo") })
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
                list += ScoreboardData.sidebarLinesFormatted.nextAfter("§9Wind Compass")
                    ?: "§7No Wind Compass for some reason"
            }

            // Better Together
            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Nearby Players:") }) {
                list += "§9Better Together"
                list += ScoreboardData.sidebarLinesFormatted.first { it.startsWith("Nearby Players:") }
            }

            // Mithril
            if (ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Event: ") }) {
                list += ScoreboardData.sidebarLinesFormatted.first { it.startsWith("Event: ") }
                    .removePrefix("Event: ") + "\n§fin " + ScoreboardData.sidebarLinesFormatted.first { it.startsWith("Zone: ") }
                    .removePrefix("Zone: ")
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
            listOf(ScoreboardData.sidebarLinesFormatted.first { it.startsWith("Protector HP: §a") || it.startsWith("Dragon HP: §a") }) +
                (ScoreboardData.sidebarLinesFormatted.first { it.startsWith("Your Damage: §c") })
        },
        {
            ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Your Damage: §c") }
        }
    ),
    ESSENCE(
        {
            listOf(ScoreboardData.sidebarLinesFormatted.first { it.startsWith("Essence: ") })
        },
        {
            ScoreboardData.sidebarLinesFormatted.any { it.startsWith("Essence: ") }
        }
    );

    fun getLines(): List<String> {
        return displayLine.get()
    }

    companion object {
        fun getFirstEvent(): Events {
            return entries.firstOrNull { it.showWhen() } ?: NONE
        }
    }
}
