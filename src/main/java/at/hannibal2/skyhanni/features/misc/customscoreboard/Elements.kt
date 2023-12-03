package at.hannibal2.skyhanni.features.misc.customscoreboard

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MaxwellAPI
import at.hannibal2.skyhanni.data.MayorElection
import at.hannibal2.skyhanni.data.PartyAPI
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.data.SlayerAPI
import at.hannibal2.skyhanni.mixins.hooks.replaceString
import at.hannibal2.skyhanni.utils.LorenzUtils.inDungeons
import at.hannibal2.skyhanni.utils.LorenzUtils.nextAfter
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.TimeUtils.formatted
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import java.util.function.Supplier

private val config get() = SkyHanniMod.feature.gui.customScoreboard

// Stats / Numbers
var purse = "0"
var motes = "0"
var bank = "0"
var bits = "0"
var copper = "0"
var gems = "0"
var location = "None"
var lobbyCode = "None"
var heat = "0"
var mithrilPowder = "0"
var gemstonePowder = "0"
var extraLines = listOf<String>()

val extraObjectiveLines = listOf("§7(§e", "§f Mages", "§f Barbarians")

enum class Elements(
    private val displayLine: Supplier<List<String>>,
    private val showWhen: () -> Boolean,
    val index: Int
) {
    SKYBLOCK(
        {
            when (config.displayConfig.useHypixelTitleAnimation) {
                true -> listOf(ScoreboardData.objectiveTitle)
                false -> listOf(config.displayConfig.customTitle.get().toString().replace("&", "§"))
            }
        },
        {
            true
        },
        0
    ),
    PROFILE(
        {
            listOf(getProfileTypeAsSymbol() + HypixelData.profileName.firstLetterUppercase())
        },
        {
            true
        },
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
        {
            !listOf(IslandType.THE_RIFT).contains(HypixelData.skyBlockIsland)
        },
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
        {
            listOf(IslandType.THE_RIFT).contains(HypixelData.skyBlockIsland)
        },
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
        {
            !listOf(IslandType.THE_RIFT).contains(HypixelData.skyBlockIsland)
        },
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
        {
            !listOf(IslandType.THE_RIFT, IslandType.CATACOMBS).contains(HypixelData.skyBlockIsland)
        },
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
        {
            listOf(IslandType.GARDEN).contains(HypixelData.skyBlockIsland)
        },
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
        {
            !listOf(IslandType.THE_RIFT, IslandType.CATACOMBS).contains(HypixelData.skyBlockIsland)
        },
        7
    ),
    HEAT(
        {
            when {
                config.informationFilteringConfig.hideEmptyLines && heat == "§c♨ 0" -> listOf("<hidden>")
                config.displayConfig.displayNumbersFirst -> listOf(if (heat == "§c♨ 0") "§c♨ 0 Heat" else "$heat Heat")
                else -> listOf(if (heat == "§c♨ 0") "Heat: §c♨ 0" else "Heat: $heat")
            }
        },
        {
            listOf(IslandType.CRYSTAL_HOLLOWS).contains(HypixelData.skyBlockIsland)
        },
        8
    ),
    EMPTY_LINE(
        {
            listOf("<empty>")
        },
        {
            true
        },
        9
    ),
    LOCATION(
        {
            listOf(replaceString(location) ?: "<hidden>")
        },
        {
            true
        },
        10
    ),
    SKYBLOCK_TIME_DAY(
        {
            listOf(SkyBlockTime.now().formatted(yearElement = false, hoursAndMinutesElement = false))
        },
        {
            true
        },
        11
    ),
    SKYBLOCK_TIME_MINUTE(
        {
            val symbols = listOf("☔", "§e☀", "§b☽")
            if (ScoreboardData.sidebarLinesFormatted.any { line -> symbols.any { line.contains(it) } }) {
                listOf(ScoreboardData.sidebarLinesFormatted.first { line -> symbols.any { line.contains(it) } })
            } else {
                listOf("§7" + SkyBlockTime.now().formatted(dayAndMonthElement = false, yearElement = false))
            }
        },
        {
            true
        },
        12
    ),
    LOBBY_CODE(
        {
            listOf("§8$lobbyCode")
        },
        {
            true
        },
        13
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
        {
            !listOf(IslandType.THE_RIFT).contains(HypixelData.skyBlockIsland)
        },
        14
    ),
    EMPTY_LINE2(
        {
            listOf("<empty>")
        },
        {
            true
        },
        15
    ),
    OBJECTIVE(
        {
            val objective = mutableListOf<String>()

            objective += "Objective:"

            objective += ScoreboardData.sidebarLinesFormatted.nextAfter("Objective") ?: "<hidden>"

            if (extraObjectiveLines.any {
                    ScoreboardData.sidebarLinesFormatted.nextAfter("Objective", 2)?.contains(it) == true
                }) {
                objective += ScoreboardData.sidebarLinesFormatted.nextAfter("Objective", 2).toString()
            }

            objective
        },
        {
            true
        },
        16
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
        {
            listOf(
                at.hannibal2.skyhanni.data.IslandType.HUB,
                at.hannibal2.skyhanni.data.IslandType.SPIDER_DEN,
                at.hannibal2.skyhanni.data.IslandType.THE_PARK,
                at.hannibal2.skyhanni.data.IslandType.THE_END,
                at.hannibal2.skyhanni.data.IslandType.CRIMSON_ISLE,
                at.hannibal2.skyhanni.data.IslandType.THE_RIFT
            ).contains(HypixelData.skyBlockIsland)
        },
        17
    ),
    EMPTY_LINE3(
        {
            listOf("<empty>")
        },
        {
            true
        },
        18
    ),
    POWDER(
        {
            when (config.displayConfig.displayNumbersFirst) {
                true -> listOf("§9§lPowder") + (" §7- §2$mithrilPowder Mithril") + (" §7- §d$gemstonePowder Gemstone")
                false -> listOf("§9§lPowder") + (" §7- §fMithril: §2$mithrilPowder") + (" §7- §fGemstone: §d$gemstonePowder")
            }
        },
        {
            listOf(IslandType.CRYSTAL_HOLLOWS, IslandType.DWARVEN_MINES).contains(HypixelData.skyBlockIsland)
        },
        19
    ),
    CURRENT_EVENT(
        {
            Events.getFirstEvent().getLines()
        },
        {
            true
        },
        20
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
        {
            !listOf(IslandType.THE_RIFT).contains(HypixelData.skyBlockIsland)
        },
        21
    ),
    PARTY(
        {
            val partyTitle: List<String> =
                if (PartyAPI.partyMembers.isEmpty() && config.informationFilteringConfig.hideEmptyLines) {
                    listOf("<hidden>")
                } else {
                    val title =
                        if (PartyAPI.partyMembers.isEmpty()) "§9§lParty" else "§9§lParty (${PartyAPI.partyMembers.size})"
                    val partyList = PartyAPI.partyMembers
                        .take(config.partyConfig.maxPartyList.get())
                        .map {
                            " §7- §7$it"
                        }
                        .toTypedArray()
                    listOf(title, *partyList)
                }

            partyTitle
        },
        {
            if (inDungeons) {
                false // Hidden bc teammate health etc exists
            } else {
                if (config.partyConfig.showPartyEverywhere) {
                    true
                } else {
                    listOf(
                        IslandType.DUNGEON_HUB,
                        IslandType.KUUDRA_ARENA,
                        IslandType.CRIMSON_ISLE
                    ).contains(HypixelData.skyBlockIsland)
                }
            }
        },
        22
    ),
    WEBSITE(
        {
            listOf(config.displayConfig.customFooter.get().toString().replace("&", "§"))
        },
        {
            true
        },
        23
    ),
    EXTRA_LINES(
        {
            listOf("§cUndetected Lines (pls report):") + extraLines
        },
        {
            extraLines.isNotEmpty()
        },
        24
    ),
    ;

    fun getLine(): List<String> {
        return displayLine.get()
    }

    fun isVisible(): Boolean {
        if (!config.informationFilteringConfig.hideIrrelevantLines) return true
        return showWhen()
    }
}
