package at.hannibal2.skyhanni.features.misc.customscoreboard

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.utils.LorenzUtils.nextAfter
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.TabListData
import java.util.function.Supplier

private val config get() = SkyHanniMod.feature.gui.customScoreboard

enum class Events(private val displayLine: Supplier<List<String>>, private val showWhen: () -> Boolean){
    NONE( // maybe use default state tablist: "Events: smth" idk
        {
            when {
                config.hideEmptyLines -> listOf("<hidden>")
                else -> listOf("§cNo Event")
            }
        },
        {
            false
        }
    ),
    DUNGEONS( // not tested
        {
            listOf("§cDungeons Event")
        },
        {
            HypixelData.skyBlockIsland == IslandType.CATACOMBS
        }
    ),
    KUUDRA( // not tested
        {
            listOf("§cKuudra Event")
        },
        {
            HypixelData.skyBlockIsland == IslandType.KUUDRA_ARENA
        }
    ),
    JACOB( // not tested
        {
            val list = mutableListOf<String>()

            // Contest
            if (ScoreboardData.sidebarLines.any { it.startsWith("§e○ §f") }) {
                list += ScoreboardData.sidebarLines.first { it.startsWith("§e○ §f") }
                list += ScoreboardData.sidebarLines.nextAfter("§e○ §f") ?: "§7No Ranking"
                list += ScoreboardData.sidebarLines.nextAfter("§e○ §f", 2) ?: "§7No Amount for next"
            }

            // Medals
            if (ScoreboardData.sidebarLines.any { it.startsWith("§6§lGOLD §fmedals:") }) {
                list += ScoreboardData.sidebarLines.first { it.startsWith("§6§lGOLD §fmedals:") }
                list += ScoreboardData.sidebarLines.first { it.startsWith("§f§lSILVER §fmedals:") }
                list += ScoreboardData.sidebarLines.first { it.startsWith("§c§lBRONZE §fmedals:") }
            }

            list
        },
        {
            ScoreboardData.sidebarLines.any { it.startsWith("§e○ §f") } || ScoreboardData.sidebarLines.any { it.startsWith("§6§lGOLD §fmedals: ") }
        }
    ),
    WINTER( // not tested
        {
            listOf("§bWinter Event")
        },
        {
            false
        }
    ),
    SPOOKY( // not tested
        {
            listOf(ScoreboardData.sidebarLines.first { it.startsWith("§6Spooky Festival§f") }) + // Time
                (getFooter().split("\n").first { it.startsWith("§r§r§7Your Candy:") }) // Candy
        },
        {
            ScoreboardData.sidebarLines.any { it.startsWith("§6Spooky Festival§f") }
        }
    ),
    MARINA( // not tested
        {
            listOf("§bFishing Festival: " + TabListData.getTabList().nextAfter("§e§lEvent: §r§bFishing Festival")?.removePrefix(" Ends In: "))
        },
        {
            TabListData.getTabList().any { it.startsWith("§e§lEvent: §r§bFishing Festival") }
        }
    ),
    NEW_YEAR( // not tested
        {
            listOf(ScoreboardData.sidebarLines.first { it.startsWith("§dNew Year Event!§f") })
        },
        {
            ScoreboardData.sidebarLines.any { it.startsWith("§dNew Year Event!§f") }
        }
    ),
    ORINGO(
        {
            listOf(ScoreboardData.sidebarLines.first { it.startsWith("§aTraveling Zoo") })
        },
        {
            ScoreboardData.sidebarLines.any { it.startsWith("§aTraveling Zoo") }
        }
    ),
    MINING_EVENTS( // not sure
        {
            val list = mutableListOf<String>()

            // Mining Fiesta
            if (TabListData.getTabList().any { it.startsWith("§6Mining Festival§f") }) {
                list += "§6Mining Fiesta: " + TabListData.getTabList().nextAfter("§e§lEvent: §r§6Mining Fiesta")?.removePrefix(" Ends In: ")
            }

            // Wind
            if (ScoreboardData.sidebarLines.any { it == "§9Wind Compass" }){
                list += "§9Wind Compass"
                list += ScoreboardData.sidebarLines.nextAfter("§9Wind Compass") ?: "§7No Wind Compass for some reason"
            }

            // Better Together
            if (ScoreboardData.sidebarLines.any { it.startsWith("Nearby Players:") }){
                list += "§9Better Together"
                list += ScoreboardData.sidebarLines.first { it.startsWith("Nearby Players:")}
            }

            // Mithril
            if (ScoreboardData.sidebarLines.any { it.startsWith("Event: ")}){
                list += ScoreboardData.sidebarLines.first { it.startsWith("Event: ")}.removePrefix("Event: ") + " §rin " + ScoreboardData.sidebarLines.first { it.startsWith("Zone: ")}.removePrefix("Zone: ")
            }

            if (list.size == 0) when (config.hideEmptyLines){
                true -> listOf("<hidden>")
                false -> listOf("§cNo Mining Event")
            } else list
        },
        {
            HypixelData.skyBlockIsland == IslandType.DWARVEN_MINES || HypixelData.skyBlockIsland == IslandType.CRYSTAL_HOLLOWS
        }
    ),
    DAMAGE( // WHY THE FUCK DOES THE REGEX NOT WORK
        {
            listOf(ScoreboardData.sidebarLines.first { it.startsWith("Protector HP: §a") || it.startsWith("Dragon HP: §a") }) + //{ "(Protector|Dragon) HP: §a\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)? §c❤".toPattern().matches(it) }) +
                (ScoreboardData.sidebarLines.first{ it.startsWith("Your Damage: §c") }) //{ "Your Damage: §c\\d{1,3}(,\\d{3})*(\\.\\d+)?".toPattern().matches(it) })
        },
        {
            ScoreboardData.sidebarLines.any { it.startsWith("Your Damage: §c") }
        }
    ),
    ESSENCE(
        {
            listOf(ScoreboardData.sidebarLines.first { it.startsWith("Essence: ") })
        },
        {
            ScoreboardData.sidebarLines.any { it.startsWith("Essence: ") }
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
