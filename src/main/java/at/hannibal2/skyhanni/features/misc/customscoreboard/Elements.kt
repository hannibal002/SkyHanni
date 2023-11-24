package at.hannibal2.skyhanni.features.misc.customscoreboard

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MaxwellAPI
import at.hannibal2.skyhanni.data.MayorElection
import at.hannibal2.skyhanni.data.PartyAPI
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.data.SlayerAPI
import at.hannibal2.skyhanni.utils.LorenzUtils.nextAfter
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.TimeUtils.formatted
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import java.util.function.Supplier

private val config get() = SkyHanniMod.feature.gui.customScoreboard

enum class Elements(
    // displayLine: The line that is displayed on the scoreboard
    private val displayLine: Supplier<List<String>>?,

    // islands: The islands that this line is displayed on
    private val islands: List<IslandType>,

    // visibilityOption: The option that is used to hide this line - use 0 to only display on the listed islands, 1 to hide on the listed islands
    private val visibilityOption: Int,

    // index: The index of the line
    val index: Int
) {
    SKYBLOCK(
        {
            listOf(config.displayConfig.customTitle.get().toString().replace("&", "§"))
        },
        listOf(),
        0,
        0
    ),
    PROFILE(
        {
            listOf(getProfileTypeAsSymbol() + HypixelData.profileName.firstLetterUppercase())
        },
        listOf(),
        0,
        1
    ),
    PURSE(
        {
            when {
                config.informationFilteringConfig.hideEmptyLines && purse == "0" -> listOf("<hidden>")
                config.displayConfig.displayNumbersFirst -> listOf("§6$purse Purse")
                else -> listOf("Purse: §6$purse")
            }
        },
        listOf(IslandType.THE_RIFT),
        1,
        2
    ),
    MOTES(
        {
            when {
                config.informationFilteringConfig.hideEmptyLines && motes == "0" -> listOf("<hidden>")
                config.displayConfig.displayNumbersFirst -> listOf("§d$motes Motes")
                else -> listOf("Motes: §d$motes")
            }
        },
        listOf(IslandType.THE_RIFT),
        0,
        3
    ),
    BANK(
        {
            when {
                config.informationFilteringConfig.hideEmptyLines && bank == "0" -> listOf("<hidden>")
                config.displayConfig.displayNumbersFirst -> listOf("§6$bank Bank")
                else -> listOf("Bank: §6$bank")
            }
        },
        listOf(IslandType.THE_RIFT),
        1,
        4
    ),
    BITS(
        {
            when {
                config.informationFilteringConfig.hideEmptyLines && bits == "0" -> listOf("<hidden>")
                config.displayConfig.displayNumbersFirst -> listOf("§b$bits Bits")
                else -> listOf("Bits: §b$bits")
            }
        },
        listOf(IslandType.THE_RIFT),
        1,
        5
    ),
    COPPER(
        {
            when {
                config.informationFilteringConfig.hideEmptyLines && copper == "0" -> listOf("<hidden>")
                config.displayConfig.displayNumbersFirst -> listOf("§c$copper Copper")
                else -> listOf("Copper: §c$copper")
            }
        },
        listOf(IslandType.GARDEN),
        0,
        6
    ),
    GEMS(
        {
            when {
                config.informationFilteringConfig.hideEmptyLines && gems == "0" -> listOf("<hidden>")
                config.displayConfig.displayNumbersFirst -> listOf("§a$gems Gems")
                else -> listOf("Gems: §a$gems")
            }
        },
        listOf(IslandType.THE_RIFT),
        1,
        7
    ),
    HEAT(
        {
            when {
                config.informationFilteringConfig.hideEmptyLines && heat == "0" -> listOf("<hidden>")
                config.displayConfig.displayNumbersFirst -> listOf(if (heat == "0") "§c♨ 0 Heat" else "§c♨ $heat Heat")
                else -> listOf(if (heat == "0") "Heat: §c♨ 0" else "Heat: $heat")
            }
        },
        listOf(IslandType.CRYSTAL_HOLLOWS),
        0,
        8
    ),
    EMPTY_LINE(
        {
            listOf("<empty>")
        },
        listOf(),
        0,
        9
    ),
    LOCATION(
        {
            listOf(location)
        },
        listOf(),
        0,
        10
    ),
    SKYBLOCK_TIME(
        {
            listOf(SkyBlockTime.now().formatted(yearElement = false,  hoursAndMinutesElement = false))
        },
        listOf(),
        0,
        11
    ),
    LOBBY_CODE(
        {
            listOf("§8$lobbyCode")
        },
        listOf(),
        0,
        12
    ),
    MAXWELL(
        {
            when (MaxwellAPI.currentPower == null) {
                true -> listOf("§c§lPlease visit Maxwell!")
                false ->
                    when (config.displayConfig.displayNumbersFirst) {
                        true -> listOf("${MaxwellAPI.currentPower?.power} Power")
                        false -> listOf("Power: ${MaxwellAPI.currentPower?.power}")
                    }
            }
        },
        listOf(IslandType.THE_RIFT),
        1,
        13
    ),
    EMPTY_LINE2(
        {
            listOf("<empty>")
        },
        listOf(),
        0,
        14
    ),
    OBJECTIVE(
        {
            when(config.informationFilteringConfig.hideEmptyLines){
                true -> listOf("Objective:") + (ScoreboardData.sidebarLinesFormatted.nextAfter("Objective") ?: "<hidden>")
                false -> listOf("Objective:") + (ScoreboardData.sidebarLinesFormatted.nextAfter("Objective") ?: "§cNo objective")
            }
        },
        listOf(),
        0,
        15
    ),
    SLAYER(
        {
            listOf(
                (if (SlayerAPI.hasActiveSlayerQuest()) "§cSlayer" else "<hidden>")
            ) + (
                " §7- §e${SlayerAPI.latestSlayerCategory.trim()}"
                ) + (
                " §7- §e${SlayerAPI.latestSlayerProgress.trim()}"
                )
        },
        listOf(
            IslandType.HUB,
            IslandType.SPIDER_DEN,
            IslandType.THE_PARK,
            IslandType.THE_END,
            IslandType.CRIMSON_ISLE,
            IslandType.THE_RIFT
        ),
        0,
        16
    ),
    EMPTY_LINE3(
        {
            listOf("<empty>")
        },
        listOf(),
        0,
        17
    ),
    POWDER(
        {
            when (config.displayConfig.displayNumbersFirst) {
                true -> listOf("§9§lPowder") + (" §7- §2$mithrilPowder Mithril") + (" §7- §d$gemstonePowder Gemstone")
                false -> listOf("§9§lPowder") + (" §7- §fMithril: §2$mithrilPowder") + (" §7- §fGemstone: §d$gemstonePowder")
            }
        },
        listOf(IslandType.CRYSTAL_HOLLOWS, IslandType.DWARVEN_MINES),
        0,
        18
    ),
    CURRENT_EVENT(
        {
            Events.getFirstEvent().getLines()
        },
        listOf(),
        0,
        19
    ),
    MAYOR(
        {
            listOf(
                MayorElection.currentCandidate?.name?.let { translateMayorNameToColor(it) } ?: "<hidden>"
            ) + (if (config.showMayorPerks) {
                MayorElection.currentCandidate?.perks?.map { " §7- §e${it.name}" } ?: emptyList()
            } else {
                emptyList()
            })
        },
        listOf(IslandType.THE_RIFT),
        1,
        20
    ),
    PARTY(
        {
            val partyTitle: List<String> = if (PartyAPI.partyMembers.isEmpty() && config.informationFilteringConfig.hideEmptyLines) {
                listOf("<hidden>")
            } else {
                val title =
                    if (PartyAPI.partyMembers.isEmpty()) "§9§lParty" else "§9§lParty (${PartyAPI.partyMembers.size})"
                val partyList = PartyAPI.partyMembers
                    .takeWhile { partyCount < config.maxPartyList.get() }
                    .map {
                        partyCount++
                        " §7- §7$it"
                    }
                    .toTypedArray()
                listOf(title, *partyList)
            }

            partyTitle
        },
        listOf(IslandType.DUNGEON_HUB, IslandType.KUUDRA_ARENA, IslandType.CRIMSON_ISLE),
        0,
        21
    ),
    WEBSITE(
        {
            listOf(config.displayConfig.customFooter.get().toString().replace("&", "§"))
        },
        listOf(),
        0,
        22
    );

    fun getLine(): List<String> {
        return displayLine?.get() ?: emptyList()
    }

    fun isVisible(): Boolean {
        if (!config.informationFilteringConfig.hideIrrelevantLines) return true
        if (islands.isEmpty()) return true
        return when (visibilityOption) {
            0 -> islands.contains(HypixelData.skyBlockIsland)
            1 -> !islands.contains(HypixelData.skyBlockIsland)
            else -> true
        }
    }
}
