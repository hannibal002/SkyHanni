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
import at.hannibal2.skyhanni.utils.RenderUtils.AlignmentEnum
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
    private val displayPair: Supplier<List<Pair<String, AlignmentEnum>>>,
    private val showWhen: () -> Boolean,
    private val configLine: String
) {
    TITLE(
        {
            val alignment = if (config.displayConfig.centerTitleAndFooter) {
                AlignmentEnum.CENTER
            } else {
                AlignmentEnum.LEFT
            }

            when (config.displayConfig.useHypixelTitleAnimation) {
                true -> listOf(ScoreboardData.objectiveTitle to alignment)
                false -> listOf(config.displayConfig.customTitle.get().toString().replace("&", "§") to alignment)
            }
        },
        {
            true
        },
        "§6§lSKYBLOCK"
    ),
    PROFILE(
        {
            listOf(CustomScoreboardUtils.getProfileTypeSymbol() + HypixelData.profileName.firstLetterUppercase() to AlignmentEnum.LEFT)
        },
        {
            true
        },
        "§7♲ Blueberry"
    ),
    PURSE(
        {
            when {
                config.informationFilteringConfig.hideEmptyLines && purse == "0" -> listOf("<hidden>")
                config.displayConfig.displayNumbersFirst -> listOf("§6$purse Purse")
                else -> listOf("Purse: §6$purse")
            }.map { it to AlignmentEnum.LEFT }
        },
        {
            !listOf(IslandType.THE_RIFT).contains(HypixelData.skyBlockIsland)
        },
        "Purse: §652,763,737"
    ),
    MOTES(
        {
            when {
                config.informationFilteringConfig.hideEmptyLines && motes == "0" -> listOf("<hidden>")
                config.displayConfig.displayNumbersFirst -> listOf("§d$motes Motes")
                else -> listOf("Motes: §d$motes")
            }.map { it to AlignmentEnum.LEFT }
        },
        {
            listOf(IslandType.THE_RIFT).contains(HypixelData.skyBlockIsland)
        },
        "Motes: §d64,647"
    ),
    BANK(
        {
            when {
                config.informationFilteringConfig.hideEmptyLines && bank == "0" -> listOf("<hidden>")
                config.displayConfig.displayNumbersFirst -> listOf("§6$bank Bank")
                else -> listOf("Bank: §6$bank")
            }.map { it to AlignmentEnum.LEFT }
        },
        {
            !listOf(IslandType.THE_RIFT).contains(HypixelData.skyBlockIsland)
        },
        "Bank: §6249M"
    ),
    BITS(
        {
            when {
                config.informationFilteringConfig.hideEmptyLines && bits == "0" -> listOf("<hidden>")
                config.displayConfig.displayNumbersFirst -> listOf("§b$bits Bits")
                else -> listOf("Bits: §b$bits")
            }.map { it to AlignmentEnum.LEFT }
        },
        {
            !listOf(IslandType.THE_RIFT, IslandType.CATACOMBS).contains(HypixelData.skyBlockIsland)
        },
        "Bits: §b59,264"
    ),
    COPPER(
        {
            when {
                config.informationFilteringConfig.hideEmptyLines && copper == "0" -> listOf("<hidden>")
                config.displayConfig.displayNumbersFirst -> listOf("§c$copper Copper")
                else -> listOf("Copper: §c$copper")
            }.map { it to AlignmentEnum.LEFT }
        },
        {
            listOf(IslandType.GARDEN).contains(HypixelData.skyBlockIsland)
        },
        "Copper: §c23,495"
    ),
    GEMS(
        {
            when {
                config.informationFilteringConfig.hideEmptyLines && gems == "0" -> listOf("<hidden>")
                config.displayConfig.displayNumbersFirst -> listOf("§a$gems Gems")
                else -> listOf("Gems: §a$gems")
            }.map { it to AlignmentEnum.LEFT }
        },
        {
            !listOf(IslandType.THE_RIFT, IslandType.CATACOMBS).contains(HypixelData.skyBlockIsland)
        },
        "Gems: §a57,873"
    ),
    HEAT(
        {
            when {
                config.informationFilteringConfig.hideEmptyLines && heat == "§c♨ 0" -> listOf("<hidden>")
                config.displayConfig.displayNumbersFirst -> listOf(if (heat == "§c♨ 0") "§c♨ 0 Heat" else "$heat Heat")
                else -> listOf(if (heat == "§c♨ 0") "Heat: §c♨ 0" else "Heat: $heat")
            }.map { it to AlignmentEnum.LEFT }
        },
        {
            listOf(IslandType.CRYSTAL_HOLLOWS).contains(HypixelData.skyBlockIsland)
        },
        "Heat: §c♨ 0"
    ),
    EMPTY_LINE(
        {
            listOf("<empty>" to AlignmentEnum.LEFT)
        },
        {
            true
        },
        ""
    ),
    LOCATION(
        {
            listOf((replaceString(location) ?: "<hidden>") to AlignmentEnum.LEFT)
        },
        {
            true
        },
        "§7⏣ §bVillage"
    ),
    VISITING(
        {
            listOf((ScoreboardData.sidebarLinesFormatted.firstOrNull { it.startsWith(" §a✌ §") }
                ?: "<hidden>") to AlignmentEnum.LEFT)
        },
        {
            ScoreboardData.sidebarLinesFormatted.any { it.startsWith(" §a✌ §") }
        },
        " §a✌ §7(§a1§7/6)"
    ),
    DATE(
        {
            listOf(
                SkyBlockTime.now().formatted(yearElement = false, hoursAndMinutesElement = false) to AlignmentEnum.LEFT
            )
        },
        {
            true
        },
        "Late Summer 11th"
    ),
    TIME(
        {
            val symbols = listOf("☔", "§e☀", "§b☽")
            if (ScoreboardData.sidebarLinesFormatted.any { line -> symbols.any { line.contains(it) } }) {
                listOf(ScoreboardData.sidebarLinesFormatted.first { line -> symbols.any { line.contains(it) } }
                    .trim() to AlignmentEnum.LEFT)
            } else {
                listOf(
                    "§7" + SkyBlockTime.now()
                        .formatted(dayAndMonthElement = false, yearElement = false) to AlignmentEnum.LEFT
                )
            }
        },
        {
            true
        },
        "§710:40pm"
    ),
    LOBBY_CODE(
        {
            listOf("§8$lobbyCode" to AlignmentEnum.LEFT)
        },
        {
            true
        },
        "§8m77CK"
    ),
    POWER(
        {
            when (MaxwellAPI.currentPower == null) {
                true -> listOf("§c§lPlease visit Maxwell!" to AlignmentEnum.LEFT)
                false ->
                    when (config.displayConfig.displayNumbersFirst) {
                        true -> listOf("${MaxwellAPI.currentPower?.power} Power" to AlignmentEnum.LEFT)
                        false -> listOf("Power: ${MaxwellAPI.currentPower?.power}" to AlignmentEnum.LEFT)
                    }
            }
        },
        {
            !listOf(IslandType.THE_RIFT).contains(HypixelData.skyBlockIsland)
        },
        "Power: Sighted"
    ),
    EMPTY_LINE2(
        {
            listOf("<empty>" to AlignmentEnum.LEFT)
        },
        {
            true
        },
        ""
    ),
    OBJECTIVE(
        {
            val objective = mutableListOf<String>()

            objective += ScoreboardData.sidebarLinesFormatted.first { it.startsWith("Objective") }

            objective += ScoreboardData.sidebarLinesFormatted.nextAfter(objective[0]) ?: "<hidden>"

            if (extraObjectiveLines.any {
                    ScoreboardData.sidebarLinesFormatted.nextAfter(objective[0], 2)?.contains(it) == true
                }) {
                objective += ScoreboardData.sidebarLinesFormatted.nextAfter(objective[0], 2).toString()
                    .replace(")", "§7)")
            }

            objective.map { it to AlignmentEnum.LEFT }
        },
        {
            true
        },
        "Objective:\n§eUpdate SkyHanni"
    ),
    SLAYER(
        {
            listOf(
                (if (SlayerAPI.hasActiveSlayerQuest()) "§cSlayer" else "<hidden>") to AlignmentEnum.LEFT
            ) + (
                " §7- §e${SlayerAPI.latestSlayerCategory.trim()}" to AlignmentEnum.LEFT
                ) + (
                " §7- §e${SlayerAPI.latestSlayerProgress.trim()}" to AlignmentEnum.LEFT
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
        "§cSlayer\n §7- §cVoidgloom Seraph III\n §7- §e12§7/§c120 §7Kills"
    ),
    EMPTY_LINE3(
        {
            listOf("<empty>" to AlignmentEnum.LEFT)
        },
        {
            true
        },
        ""
    ),
    POWDER(
        {
            when (config.displayConfig.displayNumbersFirst) {
                true -> listOf("§9§lPowder") + (" §7- §2$mithrilPowder Mithril") + (" §7- §d$gemstonePowder Gemstone")
                false -> listOf("§9§lPowder") + (" §7- §fMithril: §2$mithrilPowder") + (" §7- §fGemstone: §d$gemstonePowder")
            }.map { it to AlignmentEnum.LEFT }
        },
        {
            listOf(IslandType.CRYSTAL_HOLLOWS, IslandType.DWARVEN_MINES).contains(HypixelData.skyBlockIsland)
        },
        "§9§lPowder\n §7- §fMithril: §254,646\n §7- §fGemstone: §d51,234"
    ),
    EVENTS(
        {
            Events.getFirstEvent().getLines().map { it to AlignmentEnum.LEFT }
        },
        {
            true
        },
        "§7Wide Range of Events\n§7(too much for this here)"
    ),
    MAYOR(
        {
            listOf(
                (MayorElection.currentCandidate?.name?.let { CustomScoreboardUtils.mayorNameToColorCode(it) }
                    ?: "<hidden>") to AlignmentEnum.LEFT
            ) + (if (config.showMayorPerks) {
                MayorElection.currentCandidate?.perks?.map { " §7- §e${it.name}" to AlignmentEnum.LEFT } ?: emptyList()
            } else {
                emptyList()
            })
        },
        {
            !listOf(IslandType.THE_RIFT).contains(HypixelData.skyBlockIsland)
        },
        "§2Diana:\n §7- §eLucky!\n §7- §eMythological Ritual\n §7- §ePet XP Buff"
    ),
    PARTY(
        {
            val partyTitle: List<Pair<String, AlignmentEnum>> =
                if (PartyAPI.partyMembers.isEmpty() && config.informationFilteringConfig.hideEmptyLines) {
                    listOf("<hidden>" to AlignmentEnum.LEFT)
                } else {
                    val title =
                        if (PartyAPI.partyMembers.isEmpty()) "§9§lParty" else "§9§lParty (${PartyAPI.partyMembers.size})"
                    val partyList = PartyAPI.partyMembers
                        .take(config.partyConfig.maxPartyList.get())
                        .map {
                            " §7- §7$it"
                        }
                        .toTypedArray()
                    listOf(title, *partyList).map { it to AlignmentEnum.LEFT }
                }

            partyTitle
        },
        {
            if (inDungeons) {
                false // Hidden bc teammate health etc. exists
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
        "§9§lParty (4):\n §7- §fhannibal2\n §7- §fMoulberry\n §7- §fVahvl\n §7- §fJ10a1n15"
    ),
    FOOTER(
        {
            val alignment = if (config.displayConfig.centerTitleAndFooter) {
                AlignmentEnum.CENTER
            } else {
                AlignmentEnum.LEFT
            }

            listOf(config.displayConfig.customFooter.get().toString().replace("&", "§") to alignment)
        },
        {
            true
        },
        "§ewww.hypixel.net"
    ),
    EXTRA(
        {
            listOf("§cUndetected Lines (pls report):" to AlignmentEnum.CENTER) + extraLines.map { it to AlignmentEnum.LEFT }
        },
        {
            extraLines.isNotEmpty()
        },
        "§7Extra lines the mod is not detecting"
    ),
    ;

    override fun toString(): String {
        return configLine
    }

    fun getPair(): List<Pair<String, AlignmentEnum>> {
        return try {
            displayPair.get()
        } catch (e: NoSuchElementException) {
            listOf("<hidden>" to AlignmentEnum.LEFT)
        }
    }

    fun isVisible(): Boolean {
        if (!config.informationFilteringConfig.hideIrrelevantLines) return true
        return showWhen()
    }
}
