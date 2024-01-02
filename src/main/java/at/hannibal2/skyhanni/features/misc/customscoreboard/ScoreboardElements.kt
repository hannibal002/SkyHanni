package at.hannibal2.skyhanni.features.misc.customscoreboard

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.BitsAPI
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MaxwellAPI
import at.hannibal2.skyhanni.data.MaxwellPowers
import at.hannibal2.skyhanni.data.MayorAPI
import at.hannibal2.skyhanni.data.PartyAPI
import at.hannibal2.skyhanni.data.PurseAPI
import at.hannibal2.skyhanni.data.QuiverAPI
import at.hannibal2.skyhanni.data.QuiverArrowType
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.data.SlayerAPI
import at.hannibal2.skyhanni.features.misc.customscoreboard.CustomScoreboardUtils.formatNum
import at.hannibal2.skyhanni.features.misc.customscoreboard.CustomScoreboardUtils.getGroupFromPattern
import at.hannibal2.skyhanni.features.misc.customscoreboard.CustomScoreboardUtils.getTitleAndFooterAlignment
import at.hannibal2.skyhanni.mixins.hooks.replaceString
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.LorenzUtils.inDungeons
import at.hannibal2.skyhanni.utils.LorenzUtils.nextAfter
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.AlignmentEnum
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.TimeUtils.formatted
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import java.util.function.Supplier

private val config get() = SkyHanniMod.feature.gui.customScoreboard
private val displayConfig get() = config.displayConfig
private val informationFilteringConfig get() = config.informationFilteringConfig

var unknownLines = listOf<String>()
var amountOfUnknownLines = 0

enum class ScoreboardElements(
    private val displayPair: Supplier<List<ScoreboardElement>>,
    private val showWhen: () -> Boolean,
    private val configLine: String
) {
    TITLE(
        ::getTitleDisplayPair,
        { true },
        "§6§lSKYBLOCK"
    ),
    PROFILE(
        ::getProfileDisplayPair,
        { true },
        "§7♲ Blueberry"
    ),
    PURSE(
        ::getPurseDisplayPair,
        ::getPurseShowWhen,
        "Purse: §652,763,737"
    ),
    MOTES(
        ::getMotesDisplayPair,
        ::getMotesShowWhen,
        "Motes: §d64,647"
    ),
    BANK(
        ::getBankDisplayPair,
        ::getBankShowWhen,
        "Bank: §6249M"
    ),
    BITS(
        ::getBitsDisplayPair,
        ::getBitsShowWhen,
        "Bits: §b59,264"
    ),
    COPPER(
        ::getCopperDisplayPair,
        ::getCopperShowWhen,
        "Copper: §c23,495"
    ),
    GEMS(
        ::getGemsDisplayPair,
        ::getGemsShowWhen,
        "Gems: §a57,873"
    ),
    HEAT(
        ::getHeatDisplayPair,
        ::getHeatShowWhen,
        "Heat: §c♨ 0"
    ),
    NORTH_STARS(
        ::getNorthStarsDisplayPair,
        ::getNorthStarsShowWhen,
        "North Stars: §d756"
    ),
    EMPTY_LINE(
        ::getEmptyLineDisplayPair,
        { true }, ""
    ),
    ISLAND(
        ::getIslandDisplayPair,
        { true },
        "§7㋖ §aHub"
    ),
    LOCATION(
        ::getLocationDisplayPair,
        { true },
        "§7⏣ §bVillage"
    ),
    VISITING(
        ::getVisitDisplayPair,
        ::getVisitShowWhen,
        " §a✌ §7(§a1§7/6)"
    ),
    DATE(
        ::getDateDisplayPair,
        { true },
        "Late Summer 11th"
    ),
    TIME(
        ::getTimeDisplayPair,
        { true },
        "§710:40pm §b☽"
    ),
    LOBBY_CODE(
        ::getLobbyDisplayPair,
        { true },
        "§8m77CK"
    ),
    POWER(
        ::getPowerDisplayPair,
        ::getPowerShowWhen,
        "Power: Sighted"
    ),
    COOKIE(
        ::getCookieDisplayPair,
        ::getCookieShowWhen,
        "§d§lCookie Buff\n §f3days, 17hours"
    ),
    EMPTY_LINE2(
        ::getEmptyLineDisplayPair,
        { true }, ""
    ),
    OBJECTIVE(
        ::getObjectiveDisplayPair,
        { true },
        "Objective:\n§eUpdate SkyHanni"
    ),
    SLAYER(
        ::getSlayerDisplayPair,
        ::getSlayerShowWhen,
        "§cSlayer\n §7- §cVoidgloom Seraph III\n §7- §e12§7/§c120 §7Kills"
    ),
    EMPTY_LINE3(
        ::getEmptyLineDisplayPair,
        { true },
        ""
    ),
    QUIVER(
        ::getQuiverDisplayPair,
        ::getQuiverShowWhen,
        "Flint Arrow: §f1,234"
    ),
    POWDER(
        ::getPowderDisplayPair,
        ::getPowderShowWhen,
        "§9§lPowder\n §7- §fMithril: §254,646\n §7- §fGemstone: §d51,234"
    ),
    EVENTS(
        ::getEventsDisplayPair,
        ::getEventsShowWhen,
        "§7Wide Range of Events\n§7(too much for this here)"
    ),
    MAYOR(
        ::getMayorDisplayPair,
        ::getMayorShowWhen,
        "§2Diana:\n §7- §eLucky!\n §7- §eMythological Ritual\n §7- §ePet XP Buff"
    ),
    PARTY(
        ::getPartyDisplayPair,
        ::getPartyShowWhen,
        "§9§lParty (4):\n §7- §fhannibal2\n §7- §fMoulberry\n §7- §fVahvl\n §7- §fJ10a1n15"
    ),
    FOOTER(
        ::getFooterDisplayPair,
        { true },
        "§ewww.hypixel.net"
    ),
    EXTRA(
        ::getExtraDisplayPair,
        ::getExtraShowWhen,
        "§cUnknown lines the mod is not detecting"
    ),
    ;

    override fun toString(): String {
        return configLine
    }

    fun getPair(): List<ScoreboardElement> {
        return try {
            displayPair.get()
        } catch (e: NoSuchElementException) {
            listOf("<hidden>" to AlignmentEnum.LEFT)
        }
    }

    fun isVisible(): Boolean {
        if (!informationFilteringConfig.hideIrrelevantLines) return true
        return showWhen()
    }
}


private fun getTitleDisplayPair() = when (displayConfig.titleAndFooter.useHypixelTitleAnimation) {
    true -> listOf(ScoreboardData.objectiveTitle to getTitleAndFooterAlignment())
    false -> listOf(
        displayConfig.titleAndFooter.customTitle.get().toString()
            .replace("&", "§") to getTitleAndFooterAlignment()
    )
}

private fun getProfileDisplayPair() =
    listOf(CustomScoreboardUtils.getProfileTypeSymbol() + HypixelData.profileName.firstLetterUppercase() to AlignmentEnum.LEFT)

private fun getPurseDisplayPair(): List<ScoreboardElement> {
    var purse = PurseAPI.currentPurse.toString().formatNum()

    val earned = getGroupFromPattern(ScoreboardData.sidebarLinesFormatted, PurseAPI.pursePattern, "earned")
        .formatNum()

    if (earned != "0") {
        purse += " §7(§e+$earned§7)§6"
    }

    return when {
        informationFilteringConfig.hideEmptyLines && purse == "0" -> listOf("<hidden>")
        displayConfig.displayNumbersFirst -> listOf("§6$purse Purse")
        else -> listOf("Purse: §6$purse")
    }.map { it to AlignmentEnum.LEFT }
}

private fun getPurseShowWhen() = !listOf(IslandType.THE_RIFT).contains(HypixelData.skyBlockIsland)

private fun getMotesDisplayPair(): List<ScoreboardElement> {
    val motes = getGroupFromPattern(ScoreboardData.sidebarLinesFormatted, ScoreboardPattern.motesPattern, "motes")
        .formatNum()

    return when {
        informationFilteringConfig.hideEmptyLines && motes == "0" -> listOf("<hidden>")
        displayConfig.displayNumbersFirst -> listOf("§d$motes Motes")
        else -> listOf("Motes: §d$motes")
    }.map { it to AlignmentEnum.LEFT }
}

private fun getMotesShowWhen() = listOf(IslandType.THE_RIFT).contains(HypixelData.skyBlockIsland)

private fun getBankDisplayPair(): List<ScoreboardElement> {
    val bank = getGroupFromPattern(TabListData.getTabList(), ScoreboardPattern.bankPattern, "bank")

    return when {
        informationFilteringConfig.hideEmptyLines && bank == "0" -> listOf("<hidden>")
        displayConfig.displayNumbersFirst -> listOf("§6$bank Bank")
        else -> listOf("Bank: §6$bank")
    }.map { it to AlignmentEnum.LEFT }
}

private fun getBankShowWhen() = !listOf(IslandType.THE_RIFT).contains(HypixelData.skyBlockIsland)

private fun getBitsDisplayPair(): List<ScoreboardElement> {
    val bits = if (BitsAPI.bits == -1) {
        "0"
    } else {
        BitsAPI.bits.formatNum()
    }
    val bitsToClaim = if (BitsAPI.bitsToClaim == -1) {
        "§cOpen Sbmenu§b"
    } else if (BitsAPI.bitsToClaim < -1) {
        "0"
    } else {
        BitsAPI.bitsToClaim.formatNum()
    }

    val bitsDisplay = when {
        informationFilteringConfig.hideEmptyLines && bits == "0" -> listOf("<hidden>")
        displayConfig.displayNumbersFirst -> {
            val bitsText = if (displayConfig.showUnclaimedBits) {
                "§b$bits§7/${if (bitsToClaim == "0") "§30" else "§b${bitsToClaim}"} §bBits"
            } else {
                "§b$bits Bits"
            }
            listOf(bitsText)
        }

        else -> {
            val bitsText = if (displayConfig.showUnclaimedBits) {
                "Bits: §b$bits§7/${if (bitsToClaim == "0") "§30" else "§b${bitsToClaim}"}"
            } else {
                "Bits: §b$bits"
            }
            listOf(bitsText)
        }
    }
    return bitsDisplay.map { it to AlignmentEnum.LEFT }
}

private fun getBitsShowWhen(): Boolean {
    if (HypixelData.bingo) return false

    return !listOf(IslandType.CATACOMBS).contains(HypixelData.skyBlockIsland)
}

private fun getCopperDisplayPair(): List<ScoreboardElement> {
    val copper = getGroupFromPattern(ScoreboardData.sidebarLinesFormatted, ScoreboardPattern.copperPattern, "copper")
        .formatNum()

    return when {
        informationFilteringConfig.hideEmptyLines && copper == "0" -> listOf("<hidden>")
        displayConfig.displayNumbersFirst -> listOf("§c$copper Copper")
        else -> listOf("Copper: §c$copper")
    }.map { it to AlignmentEnum.LEFT }
}

private fun getCopperShowWhen() = listOf(IslandType.GARDEN).contains(HypixelData.skyBlockIsland)

private fun getGemsDisplayPair(): List<ScoreboardElement> {
    val gems = getGroupFromPattern(TabListData.getTabList(), ScoreboardPattern.gemsPattern, "gems")

    return when {
        informationFilteringConfig.hideEmptyLines && gems == "0" -> listOf("<hidden>")
        displayConfig.displayNumbersFirst -> listOf("§a$gems Gems")
        else -> listOf("Gems: §a$gems")
    }.map { it to AlignmentEnum.LEFT }
}

private fun getGemsShowWhen() = !listOf(IslandType.THE_RIFT, IslandType.CATACOMBS).contains(HypixelData.skyBlockIsland)

private fun getHeatDisplayPair(): List<ScoreboardElement> {
    val heat = getGroupFromPattern(ScoreboardData.sidebarLinesFormatted, ScoreboardPattern.heatPattern, "heat")

    return when {
        informationFilteringConfig.hideEmptyLines && heat == "§c♨ 0" -> listOf("<hidden>")
        displayConfig.displayNumbersFirst -> listOf(if (heat == "0") "§c♨ 0 Heat" else "$heat Heat")
        else -> listOf(if (heat == "0") "Heat: §c♨ 0" else "Heat: $heat")
    }.map { it to AlignmentEnum.LEFT }
}

private fun getHeatShowWhen() = listOf(IslandType.CRYSTAL_HOLLOWS).contains(HypixelData.skyBlockIsland)
    && ScoreboardData.sidebarLinesFormatted.any { ScoreboardPattern.heatPattern.matches(it) }

private fun getNorthStarsDisplayPair(): List<ScoreboardElement> {
    val northStars =
        getGroupFromPattern(ScoreboardData.sidebarLinesFormatted, ScoreboardPattern.northstarsPattern, "northstars")
            .formatNum()

    return when {
        informationFilteringConfig.hideEmptyLines && northStars == "0" -> listOf("<hidden>")
        displayConfig.displayNumbersFirst -> listOf("§d$northStars North Stars")
        else -> listOf("North Stars: §d$northStars")
    }.map { it to AlignmentEnum.LEFT }
}

private fun getNorthStarsShowWhen() =
    ScoreboardData.sidebarLinesFormatted.any { ScoreboardPattern.northstarsPattern.matches(it) }

private fun getEmptyLineDisplayPair() = listOf("<empty>" to AlignmentEnum.LEFT)

private fun getIslandDisplayPair() =
    listOf("§7㋖ §a" + HypixelData.skyBlockIsland.toString().split("_")
        .joinToString(" ") { it.firstLetterUppercase() } to AlignmentEnum.LEFT)

private fun getLocationDisplayPair() =
    listOf(
        (replaceString(
            getGroupFromPattern(
                ScoreboardData.sidebarLinesFormatted,
                ScoreboardPattern.locationPattern,
                "location"
            )
        )?.trim()
            ?: "<hidden>") to AlignmentEnum.LEFT
    )


private fun getVisitDisplayPair() =
    listOf(
        ScoreboardData.sidebarLinesFormatted.first { ScoreboardPattern.visitingPattern.matches(it) } to AlignmentEnum.LEFT
    )

private fun getVisitShowWhen() =
    ScoreboardData.sidebarLinesFormatted.any { ScoreboardPattern.visitingPattern.matches(it) }

private fun getDateDisplayPair() =
    listOf(SkyBlockTime.now().formatted(yearElement = false, hoursAndMinutesElement = false) to AlignmentEnum.LEFT)


private fun getTimeDisplayPair(): List<ScoreboardElement> {
    var symbol = getGroupFromPattern(ScoreboardData.sidebarLinesFormatted, ScoreboardPattern.timePattern, "symbol")
    if (symbol == "0") symbol = ""
    return listOf(
        "§7" + SkyBlockTime.now()
            .formatted(dayAndMonthElement = false, yearElement = false) + " $symbol" to AlignmentEnum.LEFT
    )
}

private fun getLobbyDisplayPair(): List<ScoreboardElement> {
    val lobbyCode = getGroupFromPattern(
        ScoreboardData.sidebarLinesFormatted,
        ScoreboardPattern.lobbyCodePattern,
        "code"
    )

    val displayValue = if (lobbyCode == "0") "<hidden>" else "§8$lobbyCode"
    return listOf(displayValue to AlignmentEnum.LEFT)
}

private fun getPowerDisplayPair() = when (MaxwellAPI.currentPower) {
    MaxwellPowers.UNKNOWN -> listOf("§cOpen \"Your Bags\"!" to AlignmentEnum.LEFT)
    null -> listOf("§cOpen \"Your Bags\"!" to AlignmentEnum.LEFT)
    else ->
        when (displayConfig.displayNumbersFirst) {
            true -> listOf(
                "${MaxwellAPI.currentPower?.power?.replace("Power","")} Power " +
                    "§7(§6${MaxwellAPI.magicalPower}§7)" to AlignmentEnum.LEFT
            )

            false -> listOf(
                "Power: ${MaxwellAPI.currentPower?.power?.replace("Power","")} " +
                    "§7(§6${MaxwellAPI.magicalPower}§7)" to AlignmentEnum.LEFT
            )
        }
}

private fun getPowerShowWhen() = !listOf(IslandType.THE_RIFT).contains(HypixelData.skyBlockIsland)

private fun getCookieDisplayPair(): List<ScoreboardElement> {
    val timeLine = CustomScoreboardUtils.getTablistFooter().split("\n")
        .nextAfter("§d§lCookie Buff") ?: "<hidden>"

    return listOf(
        "§d§lCookie Buff" to AlignmentEnum.LEFT
    ) + when (timeLine.contains("Not active")) {
        true -> listOf(" §7- §cNot active" to AlignmentEnum.LEFT)
        false -> listOf(" §7- §e${timeLine.substringAfter("§d§lCookie Buff").trim()}" to AlignmentEnum.LEFT)
    }
}

private fun getCookieShowWhen(): Boolean {
    if (HypixelData.bingo) return false

    return when (informationFilteringConfig.hideEmptyLines) {
        true -> CustomScoreboardUtils.getTablistFooter().split("\n").any {
            CustomScoreboardUtils.getTablistFooter().split("\n").nextAfter("§d§lCookie Buff")?.contains(it)
                ?: false
        }

        false -> true
    }
}

private fun getObjectiveDisplayPair(): List<ScoreboardElement> {
    val objective = mutableListOf<String>()

    objective += ScoreboardData.sidebarLinesFormatted.first { ScoreboardPattern.objectivePattern.matches(it) }

    objective += ScoreboardData.sidebarLinesFormatted.nextAfter(objective[0]) ?: "<hidden>"

    if (ScoreboardData.sidebarLinesFormatted.any { ScoreboardPattern.thirdObjectiveLinePattern.matches(it) }) {
        objective += ScoreboardData.sidebarLinesFormatted.nextAfter(objective[0], 2).toString()
    }

    return objective.map { it to AlignmentEnum.LEFT }
}

private fun getSlayerDisplayPair(): List<ScoreboardElement> {
    return listOf(
        (if (SlayerAPI.hasActiveSlayerQuest()) "§cSlayer" else "<hidden>") to AlignmentEnum.LEFT
    ) + (
        " §7- §e${SlayerAPI.latestSlayerCategory.trim()}" to AlignmentEnum.LEFT
        ) + (
        " §7- §e${SlayerAPI.latestSlayerProgress.trim()}" to AlignmentEnum.LEFT
        )
}

private fun getSlayerShowWhen() = listOf(
    IslandType.HUB,
    IslandType.SPIDER_DEN,
    IslandType.THE_PARK,
    IslandType.THE_END,
    IslandType.CRIMSON_ISLE,
    IslandType.THE_RIFT
).contains(HypixelData.skyBlockIsland)

private fun getQuiverDisplayPair(): List<ScoreboardElement> {
    if (QuiverAPI.currentArrow == null) return listOf("§cChange your Arrow once" to AlignmentEnum.LEFT)
    if (QuiverAPI.currentArrow == QuiverArrowType.NONE) return listOf("No Arrows selected" to AlignmentEnum.LEFT)
    return when (displayConfig.displayNumbersFirst) {
        true -> listOf("${QuiverAPI.currentAmount.addSeparators()} ${QuiverAPI.currentArrow?.arrow} ")
        false -> listOf("§f${QuiverAPI.currentArrow?.arrow?.replace("Arrow", "")}: " +
            "${QuiverAPI.currentAmount.addSeparators()} Arrows")
    }.map { it to AlignmentEnum.LEFT }
}

private fun getQuiverShowWhen() = !listOf(IslandType.THE_RIFT).contains(HypixelData.skyBlockIsland)

private fun getPowderDisplayPair(): List<ScoreboardElement> {
    val mithrilPowder =
        getGroupFromPattern(TabListData.getTabList(), ScoreboardPattern.mithrilPowderPattern, "mithrilpowder")
            .formatNum()
    val gemstonePowder =
        getGroupFromPattern(TabListData.getTabList(), ScoreboardPattern.gemstonePowderPattern, "gemstonepowder")
            .formatNum()

    return when (displayConfig.displayNumbersFirst) {
        true -> listOf("§9§lPowder") + (" §7- §2$mithrilPowder Mithril") + (" §7- §d$gemstonePowder Gemstone")
        false -> listOf("§9§lPowder") + (" §7- §fMithril: §2$mithrilPowder") + (" §7- §fGemstone: §d$gemstonePowder")
    }.map { it to AlignmentEnum.LEFT }
}

private fun getPowderShowWhen() =
    listOf(IslandType.CRYSTAL_HOLLOWS, IslandType.DWARVEN_MINES).contains(HypixelData.skyBlockIsland)

private fun getEventsDisplayPair(): List<ScoreboardElement> {
    if (ScoreboardEvents.getEvent().isEmpty()) return listOf("<hidden>" to AlignmentEnum.LEFT)
    return ScoreboardEvents.getEvent().flatMap { it.getLines().map { i -> i to AlignmentEnum.LEFT } }
}

private fun getEventsShowWhen() = ScoreboardEvents.getEvent().isNotEmpty()

private fun getMayorDisplayPair(): List<ScoreboardElement> {
    return listOf(
        (MayorAPI.currentMayor?.name?.let { MayorAPI.mayorNameToColorCode(it) }
            ?: "<hidden>") +
            (if (config.mayorConfig.showTimeTillNextMayor) {
                "§7 (§e${TimeUtils.formatDuration(MayorAPI.timeTillNextMayor, TimeUnit.DAY)}§7)"
            } else {
                ""
            }) to AlignmentEnum.LEFT
    ) + (if (config.mayorConfig.showMayorPerks) {
        MayorAPI.currentMayor?.perks?.map { " §7- §e${it.name}" to AlignmentEnum.LEFT } ?: emptyList()
    } else {
        emptyList()
    })
}

private fun getMayorShowWhen() =
    !listOf(IslandType.THE_RIFT).contains(HypixelData.skyBlockIsland) && MayorAPI.currentMayor != null

private fun getPartyDisplayPair() =
    if (PartyAPI.partyMembers.isEmpty() && informationFilteringConfig.hideEmptyLines) {
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

private fun getPartyShowWhen() = when (inDungeons) {
    true -> false // Hidden bc the scoreboard lines already exist
    false -> when (config.partyConfig.showPartyEverywhere) {
        true -> true
        false -> listOf(
            IslandType.DUNGEON_HUB,
            IslandType.KUUDRA_ARENA,
            IslandType.CRIMSON_ISLE
        ).contains(HypixelData.skyBlockIsland)
    }
}

private fun getFooterDisplayPair(): List<ScoreboardElement> {
    return listOf(
        displayConfig.titleAndFooter.customFooter.get().toString()
            .replace("&", "§") to getTitleAndFooterAlignment()
    )
}

private fun getExtraDisplayPair(): List<ScoreboardElement> {
    if (unknownLines.isEmpty()) return listOf("<hidden>" to AlignmentEnum.LEFT)

    if (amountOfUnknownLines != unknownLines.size && config.unknownLinesWarning) {
        ErrorManager.logErrorWithData(
            CustomScoreboardUtils.UndetectedScoreboardLines("CustomScoreboard detected ${unknownLines.size} unknown line${if (unknownLines.size > 1) "s" else ""}"),
            "CustomScoreboard detected ${unknownLines.size} unknown line${if (unknownLines.size > 1) "s" else ""}",
            "Unknown Lines" to unknownLines,
            "Island" to HypixelData.skyBlockIsland,
            "Area" to HypixelData.skyBlockArea
        )
        amountOfUnknownLines = unknownLines.size
    }

    return listOf("§cUndetected Lines:" to AlignmentEnum.LEFT) + unknownLines.map { it to AlignmentEnum.LEFT }
}

private fun getExtraShowWhen(): Boolean {
    if (unknownLines.isEmpty()) {
        amountOfUnknownLines = 0
    }
    return unknownLines.isNotEmpty()
}
