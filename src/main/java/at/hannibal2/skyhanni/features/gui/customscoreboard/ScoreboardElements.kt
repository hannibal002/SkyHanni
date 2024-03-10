package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.gui.customscoreboard.DisplayConfig.ArrowAmountDisplay
import at.hannibal2.skyhanni.data.BitsAPI
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MaxwellAPI
import at.hannibal2.skyhanni.data.MayorAPI
import at.hannibal2.skyhanni.data.PartyAPI
import at.hannibal2.skyhanni.data.PurseAPI
import at.hannibal2.skyhanni.data.QuiverAPI
import at.hannibal2.skyhanni.data.QuiverAPI.asArrowPercentage
import at.hannibal2.skyhanni.data.QuiverAPI.getArrowByNameOrNull
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.data.SlayerAPI
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.formatNum
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getGroupFromPattern
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getTitleAndFooterAlignment
import at.hannibal2.skyhanni.mixins.hooks.tryToReplaceScoreboardLine
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.CollectionUtils.nextAfter
import at.hannibal2.skyhanni.utils.LorenzUtils.inAnyIsland
import at.hannibal2.skyhanni.utils.LorenzUtils.inDungeons
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.percentageColor
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.TimeUtils.formatted
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import java.util.function.Supplier

private val config get() = SkyHanniMod.feature.gui.customScoreboard
private val displayConfig get() = config.displayConfig
private val informationFilteringConfig get() = config.informationFilteringConfig

var unknownLines = listOf<String>()
var amountOfUnknownLines = 0

enum class ScoreboardElement(
    private val displayPair: Supplier<List<ScoreboardElementType>>,
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
        ::getObjectiveShowWhen,
        "Objective:\n§eStar SkyHanni on Github"
    ),
    SLAYER(
        ::getSlayerDisplayPair,
        ::getSlayerShowWhen,
        "Slayer Quest\n §7- §cVoidgloom Seraph III\n §7- §e12§7/§c120 §7Kills"
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
        "§7Wide Range of Events\n§7(too much to show all)"
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

    fun getPair(): List<ScoreboardElementType> {
        return try {
            displayPair.get()
        } catch (e: NoSuchElementException) {
            listOf("<hidden>" to HorizontalAlignment.LEFT)
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
    listOf(CustomScoreboardUtils.getProfileTypeSymbol() + HypixelData.profileName.firstLetterUppercase() to HorizontalAlignment.LEFT)

private fun getPurseDisplayPair(): List<ScoreboardElementType> {
    var purse = PurseAPI.currentPurse.formatNum()

    val earned = getGroupFromPattern(ScoreboardData.sidebarLinesFormatted, PurseAPI.coinsPattern, "earned")

    if (earned != "0") {
        purse += " §7(§e+$earned§7)§6"
    }

    return when {
        informationFilteringConfig.hideEmptyLines && purse == "0" -> listOf("<hidden>")
        displayConfig.displayNumbersFirst -> listOf("§6$purse Purse")
        else -> listOf("Purse: §6$purse")
    }.map { it to HorizontalAlignment.LEFT }
}

private fun getPurseShowWhen() = !inAnyIsland(IslandType.THE_RIFT)

private fun getMotesDisplayPair(): List<ScoreboardElementType> {
    val motes = getGroupFromPattern(ScoreboardData.sidebarLinesFormatted, ScoreboardPattern.motesPattern, "motes")
        .formatNum()

    return when {
        informationFilteringConfig.hideEmptyLines && motes == "0" -> listOf("<hidden>")
        displayConfig.displayNumbersFirst -> listOf("§d$motes Motes")
        else -> listOf("Motes: §d$motes")
    }.map { it to HorizontalAlignment.LEFT }
}

private fun getMotesShowWhen() = inAnyIsland(IslandType.THE_RIFT)

private fun getBankDisplayPair(): List<ScoreboardElementType> {
    val bank = getGroupFromPattern(TabListData.getTabList(), ScoreboardPattern.bankPattern, "bank")

    return when {
        informationFilteringConfig.hideEmptyLines && bank == "0" -> listOf("<hidden>")
        displayConfig.displayNumbersFirst -> listOf("§6$bank Bank")
        else -> listOf("Bank: §6$bank")
    }.map { it to HorizontalAlignment.LEFT }
}

private fun getBankShowWhen() = !inAnyIsland(IslandType.THE_RIFT)

private fun getBitsDisplayPair(): List<ScoreboardElementType> {
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
    return bitsDisplay.map { it to HorizontalAlignment.LEFT }
}

private fun getBitsShowWhen(): Boolean {
    if (HypixelData.bingo) return false

    return !inAnyIsland(IslandType.CATACOMBS)
}

private fun getCopperDisplayPair(): List<ScoreboardElementType> {
    val copper = getGroupFromPattern(ScoreboardData.sidebarLinesFormatted, ScoreboardPattern.copperPattern, "copper")
        .formatNum()

    return when {
        informationFilteringConfig.hideEmptyLines && copper == "0" -> listOf("<hidden>")
        displayConfig.displayNumbersFirst -> listOf("§c$copper Copper")
        else -> listOf("Copper: §c$copper")
    }.map { it to HorizontalAlignment.LEFT }
}

private fun getCopperShowWhen() = inAnyIsland(IslandType.GARDEN)

private fun getGemsDisplayPair(): List<ScoreboardElementType> {
    val gems = getGroupFromPattern(TabListData.getTabList(), ScoreboardPattern.gemsPattern, "gems")

    return when {
        informationFilteringConfig.hideEmptyLines && gems == "0" -> listOf("<hidden>")
        displayConfig.displayNumbersFirst -> listOf("§a$gems Gems")
        else -> listOf("Gems: §a$gems")
    }.map { it to HorizontalAlignment.LEFT }
}

private fun getGemsShowWhen() = !inAnyIsland(IslandType.THE_RIFT, IslandType.CATACOMBS)

private fun getHeatDisplayPair(): List<ScoreboardElementType> {
    val heat = getGroupFromPattern(ScoreboardData.sidebarLinesFormatted, ScoreboardPattern.heatPattern, "heat")

    return when {
        informationFilteringConfig.hideEmptyLines && heat == "§c♨ 0" -> listOf("<hidden>")
        displayConfig.displayNumbersFirst -> listOf(if (heat == "0") "§c♨ 0 Heat" else "$heat Heat")
        else -> listOf(if (heat == "0") "Heat: §c♨ 0" else "Heat: $heat")
    }.map { it to HorizontalAlignment.LEFT }
}

private fun getHeatShowWhen() = inAnyIsland(IslandType.CRYSTAL_HOLLOWS)
    && ScoreboardData.sidebarLinesFormatted.any { ScoreboardPattern.heatPattern.matches(it) }

private fun getNorthStarsDisplayPair(): List<ScoreboardElementType> {
    val northStars =
        getGroupFromPattern(ScoreboardData.sidebarLinesFormatted, ScoreboardPattern.northstarsPattern, "northstars")
            .formatNum()

    return when {
        informationFilteringConfig.hideEmptyLines && northStars == "0" -> listOf("<hidden>")
        displayConfig.displayNumbersFirst -> listOf("§d$northStars North Stars")
        else -> listOf("North Stars: §d$northStars")
    }.map { it to HorizontalAlignment.LEFT }
}

private fun getNorthStarsShowWhen() =
    ScoreboardData.sidebarLinesFormatted.any { ScoreboardPattern.northstarsPattern.matches(it) }

private fun getEmptyLineDisplayPair() = listOf("<empty>" to HorizontalAlignment.LEFT)

private fun getIslandDisplayPair() =
    listOf("§7㋖ §a" + HypixelData.skyBlockIsland.displayName to HorizontalAlignment.LEFT)

private fun getLocationDisplayPair() =
    listOf(
        (tryToReplaceScoreboardLine(
            getGroupFromPattern(
                ScoreboardData.sidebarLinesFormatted,
                ScoreboardPattern.locationPattern,
                "location"
            )
        )?.trim()
            ?: "<hidden>") to HorizontalAlignment.LEFT
    )


private fun getVisitDisplayPair() =
    listOf(
        ScoreboardData.sidebarLinesFormatted.first { ScoreboardPattern.visitingPattern.matches(it) } to HorizontalAlignment.LEFT
    )

private fun getVisitShowWhen() =
    ScoreboardData.sidebarLinesFormatted.any { ScoreboardPattern.visitingPattern.matches(it) }

private fun getDateDisplayPair() =
    listOf(
        SkyBlockTime.now().formatted(yearElement = false, hoursAndMinutesElement = false) to HorizontalAlignment.LEFT
    )


private fun getTimeDisplayPair(): List<ScoreboardElementType> {
    var symbol = getGroupFromPattern(ScoreboardData.sidebarLinesFormatted, ScoreboardPattern.timePattern, "symbol")
    if (symbol == "0") symbol = ""
    return listOf(
        "§7" + SkyBlockTime.now()
            .formatted(dayAndMonthElement = false, yearElement = false) + " $symbol" to HorizontalAlignment.LEFT
    )
}

private fun getLobbyDisplayPair(): List<ScoreboardElementType> {
    val lobbyCode = getGroupFromPattern(
        ScoreboardData.sidebarLinesFormatted,
        ScoreboardPattern.lobbyCodePattern,
        "code"
    )

    val displayValue = if (lobbyCode == "0") "<hidden>" else "§8$lobbyCode"
    return listOf(displayValue to HorizontalAlignment.LEFT)
}

private fun getPowerDisplayPair() = when (MaxwellAPI.currentPower) {
    null -> listOf("§cOpen \"Your Bags\"!" to HorizontalAlignment.LEFT)
    else ->
        when (displayConfig.displayNumbersFirst) {
            true -> listOf(
                "${MaxwellAPI.currentPower?.replace("Power", "")} Power " +
                    "§7(§6${MaxwellAPI.magicalPower}§7)" to HorizontalAlignment.LEFT
            )

            false -> listOf(
                "Power: ${MaxwellAPI.currentPower?.replace("Power", "")} " +
                    "§7(§6${MaxwellAPI.magicalPower?.addSeparators()}§7)" to HorizontalAlignment.LEFT
            )
        }
}

private fun getPowerShowWhen() = !inAnyIsland(IslandType.THE_RIFT)

private fun getCookieDisplayPair(): List<ScoreboardElementType> {
    val timeLine = CustomScoreboardUtils.getTablistFooter().split("\n")
        .nextAfter("§d§lCookie Buff") ?: "<hidden>"

    return listOf(
        "§d§lCookie Buff" to HorizontalAlignment.LEFT
    ) + when (timeLine.contains("Not active")) {
        true -> listOf(" §7- §cNot active" to HorizontalAlignment.LEFT)
        false -> listOf(" §7- §e${timeLine.substringAfter("§d§lCookie Buff").trim()}" to HorizontalAlignment.LEFT)
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

private fun getObjectiveDisplayPair(): List<ScoreboardElementType> {
    val objective = mutableListOf<String>()

    objective += ScoreboardData.sidebarLinesFormatted.first { ScoreboardPattern.objectivePattern.matches(it) }

    objective += ScoreboardData.sidebarLinesFormatted.nextAfter(objective[0]) ?: "<hidden>"

    if (ScoreboardData.sidebarLinesFormatted.any { ScoreboardPattern.thirdObjectiveLinePattern.matches(it) }) {
        objective += ScoreboardData.sidebarLinesFormatted.nextAfter(objective[0], 2) ?: "Second objective here"
    }

    return objective.map { it to HorizontalAlignment.LEFT }
}

private fun getObjectiveShowWhen(): Boolean {
    if (inAnyIsland(IslandType.KUUDRA_ARENA)) return false
    return ScoreboardData.sidebarLinesFormatted.any { ScoreboardPattern.objectivePattern.matches(it) }
}

private fun getSlayerDisplayPair(): List<ScoreboardElementType> {
    return listOf(
        (if (SlayerAPI.hasActiveSlayerQuest()) "Slayer Quest" else "<hidden>") to HorizontalAlignment.LEFT
    ) + (
        " §7- §e${SlayerAPI.latestSlayerCategory.trim()}" to HorizontalAlignment.LEFT
        ) + (
        " §7- §e${SlayerAPI.latestSlayerProgress.trim()}" to HorizontalAlignment.LEFT
        )
}

private fun getSlayerShowWhen() = inAnyIsland(
    IslandType.HUB,
    IslandType.SPIDER_DEN,
    IslandType.THE_PARK,
    IslandType.THE_END,
    IslandType.CRIMSON_ISLE,
    IslandType.THE_RIFT
)

private fun getQuiverDisplayPair(): List<ScoreboardElementType> {
    if (QuiverAPI.currentArrow == null)
        return listOf("§cChange your Arrow once" to HorizontalAlignment.LEFT)
    if (QuiverAPI.currentArrow == getArrowByNameOrNull("NONE".asInternalName()))
        return listOf("No Arrows selected" to HorizontalAlignment.LEFT)

    val amountString = (if (displayConfig.colorArrowAmount) {
        percentageColor(QuiverAPI.currentAmount.toLong(), QuiverAPI.MAX_ARROW_AMOUNT.toLong()).getChatColor()
    } else {
        ""
    }) + when (displayConfig.arrowAmountDisplay) {
        ArrowAmountDisplay.NUMBER -> QuiverAPI.currentAmount.addSeparators()
        ArrowAmountDisplay.PERCENTAGE -> "${QuiverAPI.currentAmount.asArrowPercentage()}%"
        else -> QuiverAPI.currentAmount.addSeparators()
    }

    return when (displayConfig.displayNumbersFirst) {
        true -> listOf("$amountString ${QuiverAPI.currentArrow?.arrow}s")
        false -> listOf(
            "§f${QuiverAPI.currentArrow?.arrow?.replace(" Arrow", "")}: " +
                "$amountString Arrows"
        )
    }.map { it to HorizontalAlignment.LEFT }
}

private fun getQuiverShowWhen(): Boolean {
    if (informationFilteringConfig.hideIrrelevantLines && !QuiverAPI.hasBowInInventory()) return false
    return !inAnyIsland(IslandType.THE_RIFT)
}

private fun getPowderDisplayPair(): List<ScoreboardElementType> {
    val mithrilPowder =
        getGroupFromPattern(TabListData.getTabList(), ScoreboardPattern.mithrilPowderPattern, "mithrilpowder")
            .formatNum()
    val gemstonePowder =
        getGroupFromPattern(TabListData.getTabList(), ScoreboardPattern.gemstonePowderPattern, "gemstonepowder")
            .formatNum()

    return when (displayConfig.displayNumbersFirst) {
        true -> listOf("§9§lPowder") + (" §7- §2$mithrilPowder Mithril") + (" §7- §d$gemstonePowder Gemstone")
        false -> listOf("§9§lPowder") + (" §7- §fMithril: §2$mithrilPowder") + (" §7- §fGemstone: §d$gemstonePowder")
    }.map { it to HorizontalAlignment.LEFT }
}

private fun getPowderShowWhen() =
    listOf(IslandType.CRYSTAL_HOLLOWS, IslandType.DWARVEN_MINES).contains(HypixelData.skyBlockIsland)

private fun getEventsDisplayPair(): List<ScoreboardElementType> {
    if (ScoreboardEvents.getEvent().isEmpty()) return listOf("<hidden>" to HorizontalAlignment.LEFT)
    if (ScoreboardEvents.getEvent().flatMap { it.getLines() }
            .isEmpty()) return listOf("<hidden>" to HorizontalAlignment.LEFT)
    return ScoreboardEvents.getEvent().flatMap { it.getLines().map { i -> i to HorizontalAlignment.LEFT } }
}

private fun getEventsShowWhen() = ScoreboardEvents.getEvent().isNotEmpty()

private fun getMayorDisplayPair(): List<ScoreboardElementType> {
    return listOf(
        (MayorAPI.currentMayor?.mayorName?.let { MayorAPI.mayorNameWithColorCode(it) }
            ?: "<hidden>") +
            (if (config.mayorConfig.showTimeTillNextMayor) {
                "§7 (§e${MayorAPI.timeTillNextMayor.format()}§7)"
            } else {
                ""
            }) to HorizontalAlignment.LEFT
    ) + (if (config.mayorConfig.showMayorPerks) {
        MayorAPI.currentMayor?.activePerks?.map { " §7- §e${it.perkName}" to HorizontalAlignment.LEFT } ?: emptyList()
    } else {
        emptyList()
    })
}

private fun getMayorShowWhen() =
    !inAnyIsland(IslandType.THE_RIFT) && MayorAPI.currentMayor != null

private fun getPartyDisplayPair() =
    if (PartyAPI.partyMembers.isEmpty() && informationFilteringConfig.hideEmptyLines) {
        listOf("<hidden>" to HorizontalAlignment.LEFT)
    } else {
        val title =
            if (PartyAPI.partyMembers.isEmpty()) "§9§lParty" else "§9§lParty (${PartyAPI.partyMembers.size})"
        val partyList = PartyAPI.partyMembers
            .take(config.partyConfig.maxPartyList.get())
            .map {
                " §7- §7$it"
            }
            .toTypedArray()
        listOf(title, *partyList).map { it to HorizontalAlignment.LEFT }
    }

private fun getPartyShowWhen() = when (inDungeons) {
    true -> false // Hidden bc the scoreboard lines already exist
    false -> when (config.partyConfig.showPartyEverywhere) {
        true -> true
        false -> inAnyIsland(
            IslandType.DUNGEON_HUB,
            IslandType.KUUDRA_ARENA,
            IslandType.CRIMSON_ISLE
        )
    }
}

private fun getFooterDisplayPair(): List<ScoreboardElementType> {
    return listOf(
        displayConfig.titleAndFooter.customFooter.get().toString()
            .replace("&", "§") to getTitleAndFooterAlignment()
    )
}

private fun getExtraDisplayPair(): List<ScoreboardElementType> {
    if (unknownLines.isEmpty()) return listOf("<hidden>" to HorizontalAlignment.LEFT)

    if (amountOfUnknownLines != unknownLines.size && config.unknownLinesWarning) {
        ErrorManager.logErrorWithData(
            CustomScoreboardUtils.UndetectedScoreboardLines("CustomScoreboard detected ${unknownLines.size} unknown line${if (unknownLines.size > 1) "s" else ""}"),
            "CustomScoreboard detected ${unknownLines.size} unknown line${if (unknownLines.size > 1) "s" else ""}",
            "Unknown Lines" to unknownLines,
            "Island" to HypixelData.skyBlockIsland,
            "Area" to HypixelData.skyBlockArea,
            noStackTrace = true
        )
        amountOfUnknownLines = unknownLines.size
    }

    return listOf("§cUndetected Lines:" to HorizontalAlignment.LEFT) + unknownLines.map { it to HorizontalAlignment.LEFT }
}

private fun getExtraShowWhen(): Boolean {
    if (unknownLines.isEmpty()) {
        amountOfUnknownLines = 0
    }
    return unknownLines.isNotEmpty()
}
