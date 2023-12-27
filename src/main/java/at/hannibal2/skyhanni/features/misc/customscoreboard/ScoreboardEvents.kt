package at.hannibal2.skyhanni.features.misc.customscoreboard

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.misc.customscoreboard.ScoreboardEvents.VOTING
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

enum class ScoreboardEvents(private val displayLine: Supplier<List<String>>, private val showWhen: () -> Boolean) {
    VOTING(
        {
            val list = mutableListOf<String>()

            list += getSbLines().first { SbPattern.yearVotesPattern.matches(it) }

            if (getSbLines().nextAfter(list[0]) == "§7Waiting for") {
                list += "§7Waiting for"
                list += "§7your vote..."
            } else {
                if (getSbLines().any { SbPattern.votesPattern.matches(it) }) {
                    list += getSbLines().filter { SbPattern.votesPattern.matches(it) }
                }
            }

            list
        },
        {
            getSbLines().any { SbPattern.yearVotesPattern.matches(it) }
        }
    ),
    SERVER_CLOSE(
        {
            listOf(getSbLines().first { it.startsWith("§cServer closing: ") })
        },
        {
            getSbLines().any { it.startsWith("§cServer closing: ") }
        }
    ),
    DUNGEONS(
        {
            val list = mutableListOf<String>()

            list += getSbLines().first { SbPattern.autoClosingPattern.matches(it) }
            list += getSbLines().first { SbPattern.startingInPattern.matches(it) }
            list += getSbLines().first { SbPattern.keysPattern.matches(it) }
            list += getSbLines().first { SbPattern.timeElapsedPattern.matches(it) }
            list += getSbLines().first { SbPattern.clearedPattern.matches(it) }
            list += getSbLines().first { SbPattern.soloPattern.matches(it) }
            list += getSbLines().first { SbPattern.teammatesPattern.matches(it) }

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

            list += getSbLines().first { SbPattern.autoClosingPattern.matches(it) }
            list += getSbLines().first { SbPattern.startingInPattern.matches(it) }
            list += getSbLines().first { SbPattern.timeElapsedPattern.matches(it) }
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

            if (getSbLines().any { it.startsWith("Challenge: ") }) {
                list += getSbLines().first { it.startsWith("Challenge: ") }
            }

            if (getSbLines().any { it.startsWith("Points: ") }) {
                list += getSbLines().first { it.startsWith("Points: ") }
            }

            if (list.size == 0) when (config.informationFilteringConfig.hideEmptyLines) {
                true -> listOf("<hidden>")
                false -> listOf("§cNo Dojo Data")
            } else list
        },
        {
            getSbLines().any { it.startsWith("Challenge: ") }
        }
    ),
    DARK_AUCTION( // this will get an update once the darkauction islandtype pr is merged
        {
            val list = mutableListOf<String>()
            if (getSbLines().any { it.startsWith("Time Left: §b") }) {
                list += getSbLines().firstOrNull { it.startsWith("Time Left: §b") }
                    ?: "<hidden>"
            }
            list += "Current Item:"
            list += getSbLines().nextAfter("Current Item:") ?: "<hidden>"

            list
        },
        {
            getSbLines().any { it == "Current Item:" }
        }
    ),
    JACOB_CONTEST(
        {
            val list = mutableListOf<String>()

            list += "§eJacob's Contest"
            list += getSbLines().nextAfter("§eJacob's Contest") ?: "§7No Event"
            list += getSbLines().nextAfter("§eJacob's Contest", 2) ?: "§7No Ranking"
            list += getSbLines().nextAfter("§eJacob's Contest", 3) ?: "§7No Amount for next"

            list
        },
        {
            getSbLines().any { it.startsWith("§e○ §f") || it.startsWith("§6☘ §f") }
        }
    ),
    JACOB_MEDALS(
        {
            getSbLines().filter { SbPattern.medalsPattern.matches(it) }
        },
        {
            getSbLines().any { SbPattern.medalsPattern.matches(it) }
        }
    ),
    TRAPPER(
        {
            val list = mutableListOf<String>()

            if (getSbLines().any { it.startsWith("Pelts: §5") }) {
                list += getSbLines().firstOrNull { it.startsWith("Pelts: §5") }
                    ?: "<hidden>"
            }
            if (getSbLines().any { it == "Tracker Mob Location:" }) {
                list += "Tracker Mob Location:"
                list += getSbLines().nextAfter("Tracker Mob Location:") ?: "<hidden>"
            }

            list
        },
        {
            getSbLines().any { it.startsWith("Pelts: §5") || it == "Tracker Mob Location:" }
        }
    ),
    GARDEN_CLEAN_UP(
        {
            listOf(getSbLines().first { SbPattern.cleanUpPattern.matches(it) })
        },
        {
            getSbLines().any { SbPattern.cleanUpPattern.matches(it) }
        }
    ),
    GARDEN_PASTING(
        {
            listOf(getSbLines().first { SbPattern.pastingPattern.matches(it) })
        },
        {
            getSbLines().any { SbPattern.pastingPattern.matches(it) }
        }
    ),
    FLIGHT_DURATION(
        {
            listOf(
                getSbLines().firstOrNull { it.startsWith("Flight Duration:") }
                    ?: "<hidden>"
            )
        },
        {
            getSbLines().any { it.startsWith("Flight Duration:") }
        }
    ),
    WINTER(
        {
            val list = mutableListOf<String>()
            val sidebarLines = getSbLines()

            list += getSbLines().first { SbPattern.northstarsPattern.matches(it) }

            if (sidebarLines.any { it.startsWith("Event Start: §a") }) {
                list += sidebarLines.firstOrNull { it.startsWith("Event Start: §a") }
                    ?: "<hidden>"
            }
            if (sidebarLines.any { it.startsWith("Next Wave: §a") && it != "Next Wave: §aSoon!" }) {
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
            listOf(getSbLines().firstOrNull { it.startsWith("§6Spooky Festival§f") }
                ?: "<hidden>") + // Time
                ("§7Your Candy: ") +
                (CustomScoreboardUtils.getTablistFooter().split("\n").firstOrNull { it.startsWith("§7Your Candy:") }
                    ?.removePrefix("§7Your Candy:") ?: "§cCandy not found") // Candy
        },
        {
            getSbLines().any { it.startsWith("§6Spooky Festival§f") }
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
            listOf(getSbLines().first { SbPattern.broodmotherPattern.matches(it) })
        },
        {
            getSbLines().any { SbPattern.broodmotherPattern.matches(it) }
        }
    ),
    NEW_YEAR(
        {
            listOf(getSbLines().firstOrNull { it.startsWith("§dNew Year Event") } ?: "<hidden>")
        },
        {
            getSbLines().any { it.startsWith("§dNew Year Event") }
        }
    ),
    ORINGO(
        {
            listOf(getSbLines().first { SbPattern.travelingZooPattern.matches(it) })
        },
        {
            getSbLines().any { SbPattern.travelingZooPattern.matches(it) }
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
            if (getSbLines().any { SbPattern.windCompassPattern.matches(it) }) {
                list += getSbLines().first { SbPattern.windCompassPattern.matches(it) }
                list += getSbLines().first { SbPattern.windCompassArrowPattern.matches(it) }
            }

            // Better Together
            if (getSbLines().any { it.startsWith("Nearby Players:") }) {
                list += "§9Better Together"
                list += (" " + getSbLines().firstOrNull { it.startsWith("Nearby Players:") })
            }

            // Zone ScoreboardEvents
            if (getSbLines().any { it.startsWith("Event: ") } && getSbLines().any {
                    it.startsWith("Zone: ")
                }) {
                list += getSbLines().firstOrNull { it.startsWith("Event: ") }
                    ?.removePrefix("Event: ") ?: "<hidden>"
                if (getSbLines().any { it.startsWith("Zone: ") }) {
                    list += "§fin " + (getSbLines().firstOrNull { it.startsWith("Zone: ") }
                        ?.removePrefix("Zone: ") ?: "<hidden>")
                }
            }

            // Mithril Gourmand
            if (getSbLines().any { it.startsWith("Remaining: §a") } && getSbLines().any {
                    it.startsWith(
                        "Your Tasty Mithril: §c"
                    )
                }) {
                list += "§6Mithril Gourmand"
                list += getSbLines().firstOrNull { it.startsWith("Remaining: §a") }
                    ?: "<hidden>"
                list += getSbLines().firstOrNull { it.startsWith("Your Tasty Mithril: §c") }
                    ?: "<hidden>"
            }

            // raffle
            if (getSbLines().any { it.startsWith("Tickets: §a") }) {
                list += "§6Raffle"
                list += getSbLines().firstOrNull { it.startsWith("Tickets: §a") }
                    ?: "<hidden>"
                list += getSbLines().firstOrNull { it.startsWith("Pool: §6") }
                    ?: "<hidden>"
            }

            // raid
            if (getSbLines().any { it.startsWith("Remaining: §a") && it.endsWith("goblins") }) {
                list += "§cGoblin Raid"
                list += getSbLines().firstOrNull { it.startsWith("Remaining: §a") }
                    ?: "<hidden>"
                list += getSbLines().firstOrNull { it.startsWith("Your kills: §c") }
                    ?: "<hidden>"
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
            listOf(getSbLines().firstOrNull {
                it.startsWith("Protector HP: §a") || it.startsWith(
                    "Dragon HP: §a"
                )
            } ?: "<hidden>") +
                (getSbLines().firstOrNull { it.startsWith("Your Damage: §c") } ?: "<hidden>")
        },
        {
            getSbLines().any { it.startsWith("Your Damage: §c") }
        }
    ),
    MAGMA_BOSS(
        {
            val list = mutableListOf<String>()

            list += getSbLines().first { SbPattern.magmaBossPattern.matches(it) }
            list += getSbLines().first { SbPattern.damageSoakedPattern.matches(it) }
            list += getSbLines().first { SbPattern.damagedSoakedBarPattern.matches(it) }
            list += getSbLines().first { SbPattern.killMagmasPattern.matches(it) }
            list += getSbLines().first { SbPattern.killMagmasBarPattern.matches(it) }
            list += getSbLines().first { SbPattern.reformingPattern.matches(it) }
            list += getSbLines().first { SbPattern.bossHealthPattern.matches(it) }
            list += getSbLines().first { SbPattern.bossHealthBarPattern.matches(it) }

            if (list.size == 0) when (config.informationFilteringConfig.hideEmptyLines) {
                true -> listOf("<hidden>")
                false -> listOf("§cNo Magma Boss Data")
            } else
                list
        },
        {
            at.hannibal2.skyhanni.data.HypixelData.skyBlockArea == "Magma Chamber"
        }
    ),
    ESSENCE(
        {
            listOf(getSbLines().first { SbPattern.essencePattern.matches(it) })
        },
        {
            getSbLines().any { SbPattern.essencePattern.matches(it) }
        }
    ),
    EFFIGIES(
        {
            listOf(getSbLines().firstOrNull { it.startsWith("Effigies: ") } ?: "<hidden>")
        },
        {
            getSbLines().any { it.startsWith("Effigies: ") }
        }
    ),

    NONE( // maybe use default state tablist: "Events: smth"
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
        fun getEvent(): List<ScoreboardEvents> {
            if (config.displayConfig.showAllActiveEvents) {
                return entries.filter { it.showWhen() }
            }
            return listOf(entries.firstOrNull { it.showWhen() } ?: NONE)
        }
    }
}

private fun getSbLines(): List<String> {
    return ScoreboardData.sidebarLinesFormatted
}
