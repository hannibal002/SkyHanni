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
    NONE(
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
    DUNGEONS(
        {
            listOf("§cDungeons Event")
        },
        {
            HypixelData.skyBlockIsland == IslandType.CATACOMBS
        }
    ),
    KUUDRA(
        {
            listOf("§cKuudra Event")
        },
        {
            HypixelData.skyBlockIsland == IslandType.KUUDRA_ARENA
        }
    ),
    JACOB(
        {
            listOf("§cJacob Event")
        },
        {
            false
        }
    ),
    WINTER(
        {
            listOf("§bWinter Event")
        },
        {
            false
        }
    ),
    SPOOKY(
        {
            listOf(ScoreboardData.sidebarLines.first { it.startsWith("§6Spooky Festival§f") }) + // Time
                (getFooter().split("\n").first { it.startsWith("§r§r§7Your Candy:") }) // Candy
        },
        {
            ScoreboardData.sidebarLines.any { it.startsWith("§6Spooky Festival§f") }
        }
    ),
    MARINA(
        {
            listOf("§bFishing Festival: " + TabListData.getTabList().nextAfter("§e§lEvent: §r§bFishing Festival")?.removePrefix(" Ends In: "))
        },
        {
            TabListData.getTabList().any { it.startsWith("§e§lEvent: §r§bFishing Festival") }
        }
    ),
    NEW_YEAR(
        {
            listOf(ScoreboardData.sidebarLines.first { it.startsWith("§dNew Year Event!§f") })
        },
        {
            ScoreboardData.sidebarLines.any { it.startsWith("§dNew Year Event!§f") }
        }
    ),
    ORINGO(
        {
            listOf("§6Oringo Event")
        },
        {
            false
        }
    ),
    MINING_EVENTS(
        {
            val list = mutableListOf<String>()

            // Mining Fiesta
            if (TabListData.getTabList().any { it.startsWith("§6Mining Festival§f") }) {
                list += "§6Mining Fiesta: " + TabListData.getTabList().nextAfter("§e§lEvent: §r§6Mining Fiesta")?.removePrefix(" Ends In: ")
            }

            // Wind
            if (ScoreboardData.sidebarLines.first { it == "§9Wind Compass" }.isNotEmpty()){
                list += "§9Wind Compass"
                list += ScoreboardData.sidebarLines.nextAfter("§9Wind Compass") ?: "§7No Wind Compass for some reason"
            }

            // Better Together
            if (ScoreboardData.sidebarLines.first { it.startsWith("Nearby Players:") }.isNotEmpty()){
                list += "§9Better Together"
                list += ScoreboardData.sidebarLines.first { it.startsWith("Nearby Players:")}
            }

            list
        },
        {
            HypixelData.skyBlockIsland == IslandType.DWARVEN_MINES || HypixelData.skyBlockIsland == IslandType.CRYSTAL_HOLLOWS
        }
    ),
    DAMAGE(
        {
            listOf(ScoreboardData.sidebarLines.first { "(Protector|Dragon) HP: §a\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)? §c❤".toPattern().matches(it) }) +
                (ScoreboardData.sidebarLines.firstOrNull { "Your Damage: §c\\d{1,3}(,\\d{3})*(\\.\\d+)?".toPattern().matches(it) } ?: "")
        },
        {
            ScoreboardData.sidebarLines.any { "(Protector|Dragon) HP: §a\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)? §c❤".toPattern().matches(it) }
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

    fun getLine(): List<String> {
        return displayLine.get()
    }

    companion object {
        fun getFirstEvent(): Events {
            return getAllEventsToDisplay().firstOrNull() ?: NONE
        }

        fun getAllEventsToDisplay(): List<Events> {
            return entries.filter { it.showWhen.invoke() }
        }
    }
}
