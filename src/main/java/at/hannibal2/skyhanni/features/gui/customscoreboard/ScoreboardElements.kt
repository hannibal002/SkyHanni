package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.api.HotmAPI
import at.hannibal2.skyhanni.config.features.gui.customscoreboard.ArrowConfig.ArrowAmountDisplay
import at.hannibal2.skyhanni.config.features.gui.customscoreboard.DisplayConfig.PowderDisplay
import at.hannibal2.skyhanni.data.BitsAPI
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.HypixelData.getMaxPlayersForCurrentServer
import at.hannibal2.skyhanni.data.HypixelData.getPlayersOnCurrentServer
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MaxwellAPI
import at.hannibal2.skyhanni.data.MayorAPI
import at.hannibal2.skyhanni.data.MiningAPI
import at.hannibal2.skyhanni.data.MiningAPI.inGlaciteArea
import at.hannibal2.skyhanni.data.PartyAPI
import at.hannibal2.skyhanni.data.PurseAPI
import at.hannibal2.skyhanni.data.QuiverAPI
import at.hannibal2.skyhanni.data.QuiverAPI.NONE_ARROW_TYPE
import at.hannibal2.skyhanni.data.QuiverAPI.asArrowPercentage
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.data.SlayerAPI
import at.hannibal2.skyhanni.data.WinterAPI
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.features.gui.customscoreboard.ChunkedStats.Companion.getChunkedStats
import at.hannibal2.skyhanni.features.gui.customscoreboard.ChunkedStats.Companion.shouldShowChunkedStats
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.arrowConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.chunkedConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.config
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.displayConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.maxwellConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.mayorConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.partyConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.formatNumber
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.formatStringNum
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getBank
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getBitsLine
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getCopper
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getElementFromAny
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getGems
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getGroup
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getHeat
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getMotes
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getNorthStars
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getPurseEarned
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getSoulflow
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.CollectionUtils.addNotNull
import at.hannibal2.skyhanni.utils.CollectionUtils.editCopy
import at.hannibal2.skyhanni.utils.CollectionUtils.nextAfter
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.inAdvancedMiningIsland
import at.hannibal2.skyhanni.utils.LorenzUtils.inAnyIsland
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.percentageColor
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatches
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.StringUtils.pluralize
import at.hannibal2.skyhanni.utils.TimeLimitedSet
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.TimeUtils.formatted
import java.util.function.Supplier
import kotlin.time.Duration.Companion.seconds
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern as SbPattern

const val HIDDEN = "<hidden>"
const val EMPTY = "<empty>"
internal var confirmedUnknownLines = listOf<String>()
internal var unconfirmedUnknownLines = listOf<String>()
internal var unknownLinesSet = TimeLimitedSet<String>(1.seconds) { onRemoval(it) }

private fun onRemoval(line: String) {
    if (!LorenzUtils.inSkyBlock) return
    if (!unconfirmedUnknownLines.contains(line)) return
    if (line !in unconfirmedUnknownLines) return
    unconfirmedUnknownLines = unconfirmedUnknownLines.filterNot { it == line }
    confirmedUnknownLines = confirmedUnknownLines.editCopy { add(line) }
    if (!config.unknownLinesWarning) return
    val pluralize = pluralize(confirmedUnknownLines.size, "unknown line", withNumber = true)
    val message = "CustomScoreboard detected $pluralize"
    ErrorManager.logErrorWithData(
        CustomScoreboardUtils.UndetectedScoreboardLines(message),
        message,
        "Unknown Lines" to confirmedUnknownLines,
        "Island" to LorenzUtils.skyBlockIsland,
        "Area" to HypixelData.skyBlockArea,
        "Full Scoreboard" to ScoreboardData.sidebarLinesFormatted,
        noStackTrace = true,
        betaOnly = true,
    )
}

var amountOfUnknownLines = 0

enum class ScoreboardElement(
    private val displayPair: Supplier<List<Any>>,
    val showWhen: () -> Boolean,
    private val configLine: String,
) {
    TITLE(
        ::getTitleDisplayPair,
        { true },
        "§6§lSKYBLOCK",
    ),
    PROFILE(
        ::getProfileDisplayPair,
        { true },
        "§7♲ Blueberry",
    ),
    PURSE(
        ::getPurseDisplayPair,
        ::getPurseShowWhen,
        "Purse: §652,763,737",
    ),
    MOTES(
        ::getMotesDisplayPair,
        ::getMotesShowWhen,
        "Motes: §d64,647",
    ),
    BANK(
        ::getBankDisplayPair,
        ::getBankShowWhen,
        "Bank: §6249M",
    ),
    BITS(
        ::getBitsDisplayPair,
        ::getBitsShowWhen,
        "Bits: §b59,264",
    ),
    COPPER(
        ::getCopperDisplayPair,
        ::getCopperShowWhen,
        "Copper: §c23,495",
    ),
    GEMS(
        ::getGemsDisplayPair,
        ::getGemsShowWhen,
        "Gems: §a57,873",
    ),
    HEAT(
        ::getHeatDisplayPair,
        ::getHeatShowWhen,
        "Heat: §c♨ 0",
    ),
    COLD(
        ::getColdDisplayPair,
        ::getColdShowWhen,
        "Cold: §b0❄",
    ),
    NORTH_STARS(
        ::getNorthStarsDisplayPair,
        ::getNorthStarsShowWhen,
        "North Stars: §d756",
    ),
    CHUNKED_STATS(
        ::getChunkedStatsDisplayPair,
        ::shouldShowChunkedStats,
        "§652,763,737 §7| §d64,647 §7| §6249M §7| §b59,264 §7| §c23,495 §7| §a57,873 §7| §c♨ 0 §7| §b0❄ §7| §d756"
    ),
    SOULFLOW(
        ::getSoulflowDisplayPair,
        ::getSoulflowDisplayWhen,
        "Soulflow: §3761"
    ),
    EMPTY_LINE(
        ::getEmptyLineDisplayPair,
        { true },
        "",
    ),
    ISLAND(
        ::getIslandDisplayPair,
        { true },
        "§7㋖ §aHub",
    ),
    LOCATION(
        ::getLocationDisplayPair,
        { true },
        "§7⏣ §bVillage",
    ),
    PLAYER_AMOUNT(
        ::getPlayerAmountDisplayPair,
        { true },
        "§7Players: §a69§7/§a80",
    ),
    VISITING(
        ::getVisitDisplayPair,
        ::getVisitShowWhen,
        " §a✌ §7(§a1§7/6)",
    ),
    DATE(
        ::getDateDisplayPair,
        { true },
        "Late Summer 11th",
    ),
    TIME(
        ::getTimeDisplayPair,
        { true },
        "§710:40pm §b☽",
    ),
    LOBBY_CODE(
        ::getLobbyDisplayPair,
        { true },
        "§8mega77CK",
    ),
    POWER(
        ::getPowerDisplayPair,
        ::getPowerShowWhen,
        "Power: §aSighted §7(§61.263§7)",
    ),
    TUNING(
        ::getTuningDisplayPair,
        ::getPowerShowWhen,
        "Tuning: §c❁34§7, §e⚔20§7, and §9☣7",
    ),
    COOKIE(
        ::getCookieDisplayPair,
        ::getCookieShowWhen,
        "§dCookie Buff§f: 3d 17h",
    ),
    EMPTY_LINE2(
        ::getEmptyLineDisplayPair,
        { true },
        "",
    ),
    OBJECTIVE(
        ::getObjectiveDisplayPair,
        ::getObjectiveShowWhen,
        "Objective:\n§eStar SkyHanni on Github",
    ),
    SLAYER(
        ::getSlayerDisplayPair,
        ::getSlayerShowWhen,
        "Slayer Quest\n §7- §cVoidgloom Seraph III\n §7- §e12§7/§c120 §7Kills",
    ),
    EMPTY_LINE3(
        ::getEmptyLineDisplayPair,
        { true },
        "",
    ),
    QUIVER(
        ::getQuiverDisplayPair,
        ::getQuiverShowWhen,
        "Flint Arrow: §f1,234",
    ),
    POWDER(
        ::getPowderDisplayPair,
        ::getPowderShowWhen,
        "§9§lPowder\n §7- §fMithril: §254,646\n §7- §fGemstone: §d51,234",
    ),
    EVENTS(
        ::getEventsDisplayPair,
        { true },
        "§7Wide Range of Events\n§7(too much to show all)",
    ),
    MAYOR(
        ::getMayorDisplayPair,
        ::getMayorShowWhen,
        "§2Diana:\n §7- §eLucky!\n §7- §eMythological Ritual\n §7- §ePet XP Buff",
    ),
    PARTY(
        ::getPartyDisplayPair,
        ::getPartyShowWhen,
        "§9§lParty (4):\n §7- §fhannibal2\n §7- §fMoulberry\n §7- §fVahvl\n §7- §fSkirtwearer",
    ),
    FOOTER(
        ::getFooterDisplayPair,
        { true },
        "§ewww.hypixel.net",
    ),
    EXTRA(
        ::getExtraDisplayPair,
        ::getExtraShowWhen,
        "§cUnknown lines the mod is not detecting",
    ),
    EMPTY_LINE4(
        ::getEmptyLineDisplayPair,
        { true },
        "",
    ),
    EMPTY_LINE5(
        ::getEmptyLineDisplayPair,
        { true },
        "",
    ),
    EMPTY_LINE6(
        ::getEmptyLineDisplayPair,
        { true },
        "",
    ),
    EMPTY_LINE7(
        ::getEmptyLineDisplayPair,
        { true },
        "",
    ),
    EMPTY_LINE8(
        ::getEmptyLineDisplayPair,
        { true },
        "",
    ),
    EMPTY_LINE9(
        ::getEmptyLineDisplayPair,
        { true },
        "",
    ),
    EMPTY_LINE10(
        ::getEmptyLineDisplayPair,
        { true },
        "",
    ),
    ;

    override fun toString() = configLine

    fun getVisiblePair() = if (isVisible()) getPair() else listOf(HIDDEN to HorizontalAlignment.LEFT)

    private fun getPair(): List<ScoreboardElementType> = displayPair.get().map { getElementFromAny(it) }

    private fun isVisible(): Boolean {
        if (!informationFilteringConfig.hideIrrelevantLines) return true
        return showWhen()
    }

    companion object {
        // I don't know why, but this field is needed for it to work
        @JvmField
        val defaultOption = listOf(
            TITLE,
            PROFILE,
            PURSE,
            BANK,
            MOTES,
            BITS,
            COPPER,
            NORTH_STARS,
            HEAT,
            COLD,
            EMPTY_LINE,
            ISLAND,
            LOCATION,
            LOBBY_CODE,
            PLAYER_AMOUNT,
            VISITING,
            EMPTY_LINE2,
            DATE,
            TIME,
            EVENTS,
            OBJECTIVE,
            COOKIE,
            EMPTY_LINE3,
            QUIVER,
            POWER,
            TUNING,
            EMPTY_LINE4,
            POWDER,
            MAYOR,
            PARTY,
            FOOTER,
            EXTRA,
        )
    }
}

private fun getProfileDisplayPair() = listOf(CustomScoreboardUtils.getProfileTypeSymbol() + HypixelData.profileName.firstLetterUppercase())

private fun getPurseDisplayPair(): List<String> {
    var purse = formatNumber(PurseAPI.currentPurse)

    if (!displayConfig.hideCoinsDifference) {
        purse += getPurseEarned() ?: ""
    }

    return listOf(
        when {
            informationFilteringConfig.hideEmptyLines && purse == "0" -> HIDDEN
            displayConfig.displayNumbersFirst -> "§6$purse Purse"
            else -> "Purse: §6$purse"
        },
    )
}

private fun getPurseShowWhen() = !inAnyIsland(IslandType.THE_RIFT)

private fun getMotesDisplayPair(): List<String> {
    val motes = formatStringNum(getMotes())

    return listOf(
        when {
            informationFilteringConfig.hideEmptyLines && motes == "0" -> HIDDEN
            displayConfig.displayNumbersFirst -> "§d$motes Motes"
            else -> "Motes: §d$motes"
        },
    )
}

private fun getMotesShowWhen() = inAnyIsland(IslandType.THE_RIFT)

private fun getBankDisplayPair(): List<String> {
    val bank = getBank()

    return listOf(
        when {
            informationFilteringConfig.hideEmptyLines && (bank == "0" || bank == "0§7 / §60") -> HIDDEN
            displayConfig.displayNumbersFirst -> "§6$bank Bank"
            else -> "Bank: §6$bank"
        },
    )
}

private fun getBankShowWhen() = !inAnyIsland(IslandType.THE_RIFT)

private fun getBitsDisplayPair(): List<String> {
    val bits = formatNumber(BitsAPI.bits.coerceAtLeast(0))
    val bitsToClaim = if (BitsAPI.bitsAvailable == -1) {
        "§cOpen Sbmenu§b"
    } else {
        formatNumber(BitsAPI.bitsAvailable.coerceAtLeast(0))
    }

    return listOf(
        when {
            informationFilteringConfig.hideEmptyLines && bits == "0" && bitsToClaim == "0" -> HIDDEN
            displayConfig.displayNumbersFirst -> "${getBitsLine()} Bits"
            else -> "Bits: ${getBitsLine()}"
        },
    )
}

private fun getBitsShowWhen() = !HypixelData.bingo && !inAnyIsland(IslandType.CATACOMBS, IslandType.KUUDRA_ARENA)

private fun getCopperDisplayPair(): List<String> {
    val copper = formatStringNum(getCopper())

    return listOf(
        when {
            informationFilteringConfig.hideEmptyLines && copper == "0" -> HIDDEN
            displayConfig.displayNumbersFirst -> "§c$copper Copper"
            else -> "Copper: §c$copper"
        },
    )
}

private fun getCopperShowWhen() = inAnyIsland(IslandType.GARDEN)

private fun getGemsDisplayPair(): List<String> {
    val gems = getGems()

    return listOf(
        when {
            informationFilteringConfig.hideEmptyLines && gems == "0" -> HIDDEN
            displayConfig.displayNumbersFirst -> "§a$gems Gems"
            else -> "Gems: §a$gems"
        },
    )
}

private fun getGemsShowWhen() = !inAnyIsland(IslandType.THE_RIFT, IslandType.CATACOMBS, IslandType.KUUDRA_ARENA)

private fun getHeatDisplayPair(): List<String> {
    val heat = getHeat()

    return listOf(
        when {
            informationFilteringConfig.hideEmptyLines && heat == "§c♨ 0" -> HIDDEN
            displayConfig.displayNumbersFirst -> (heat ?: "§c♨ 0") + " Heat"
            else -> "Heat: " + (heat ?: "§c♨ 0")
        },
    )
}

private fun getHeatShowWhen() = inAnyIsland(IslandType.CRYSTAL_HOLLOWS) &&
    SbPattern.heatPattern.anyMatches(ScoreboardData.sidebarLinesFormatted)

private fun getColdDisplayPair(): List<String> {
    val cold = -MiningAPI.cold

    return listOf(
        when {
            informationFilteringConfig.hideEmptyLines && cold == 0 -> HIDDEN
            displayConfig.displayNumbersFirst -> "§b$cold❄ Cold"
            else -> "Cold: §b$cold❄"
        },
    )
}

private fun getColdShowWhen() = inAnyIsland(IslandType.DWARVEN_MINES, IslandType.MINESHAFT) &&
    SbPattern.coldPattern.anyMatches(ScoreboardData.sidebarLinesFormatted)

private fun getNorthStarsDisplayPair(): List<String> {
    val northStars = formatStringNum(getNorthStars())

    return listOf(
        when {
            informationFilteringConfig.hideEmptyLines && northStars == "0" -> HIDDEN
            displayConfig.displayNumbersFirst -> "§d$northStars North Stars"
            else -> "North Stars: §d$northStars"
        },
    )
}

private fun getNorthStarsShowWhen() = WinterAPI.inWorkshop()

private fun getChunkedStatsDisplayPair(): List<String> =
    getChunkedStats().chunked(chunkedConfig.maxStatsPerLine)
        .map { it.joinToString(" §f| ") }

private fun getSoulflowDisplayPair(): List<String> {
    val soulflow = getSoulflow()
    return listOf(
        when {
            informationFilteringConfig.hideEmptyLines && soulflow == "0" -> HIDDEN
            displayConfig.displayNumbersFirst -> "§3$soulflow Soulflow"
            else -> "Soulflow: §3$soulflow"
        },
    )
}

private fun getSoulflowDisplayWhen() = !inAnyIsland(IslandType.THE_RIFT)

private fun getEmptyLineDisplayPair() = listOf(EMPTY)

private fun getIslandDisplayPair() =
    listOf("§7㋖ §a" + LorenzUtils.skyBlockIsland.displayName to HorizontalAlignment.LEFT)

private fun getLocationDisplayPair() = buildList {
    addNotNull(HypixelData.skyBlockAreaWithSymbol)
    addNotNull(SbPattern.plotPattern.firstMatches(ScoreboardData.sidebarLinesFormatted))
}

fun getPlayerAmountDisplayPair() = buildList {
    val max = if (displayConfig.showMaxIslandPlayers) {
        "§7/§a${getMaxPlayersForCurrentServer()}"
    } else ""
    if (displayConfig.displayNumbersFirst) {
        add("§a${getPlayersOnCurrentServer()}$max Players")
    } else add("§7Players: §a${getPlayersOnCurrentServer()}$max")
}

private fun getVisitDisplayPair() = listOfNotNull(SbPattern.visitingPattern.firstMatches(ScoreboardData.sidebarLinesFormatted))

private fun getVisitShowWhen() = SbPattern.visitingPattern.anyMatches(ScoreboardData.sidebarLinesFormatted)

private fun getDateDisplayPair() = listOf(SkyBlockTime.now().formatted(yearElement = false, hoursAndMinutesElement = false))

private fun getTimeDisplayPair(): List<String> {
    val symbol = getGroup(SbPattern.timePattern, ScoreboardData.sidebarLinesFormatted, "symbol") ?: ""
    return listOf(
        "§7" + SkyBlockTime.now()
            .formatted(
                dayAndMonthElement = false,
                yearElement = false,
                timeFormat24h = displayConfig.skyblockTime24hFormat,
            ) + " $symbol",
    )
}

private fun getLobbyDisplayPair(): List<String> {
    val lobbyCode = HypixelData.serverId
    val roomId = DungeonAPI.getRoomID()?.let { "§8$it" } ?: ""
    val lobbyDisplay = lobbyCode?.let { "§8$it $roomId" } ?: HIDDEN
    return listOf(lobbyDisplay)
}

private fun getPowerDisplayPair() = listOf(
    (MaxwellAPI.currentPower?.let {
        val mp = if (maxwellConfig.showMagicalPower) "§7(§6${MaxwellAPI.magicalPower?.addSeparators()}§7)" else ""
        if (displayConfig.displayNumbersFirst) {
            "§a${it.replace(" Power", "")} Power $mp"
        } else "Power: §a$it $mp"
    } ?: "§cOpen \"Your Bags\"!"),
)

private fun getTuningDisplayPair(): List<String> {
    val tunings = MaxwellAPI.tunings ?: return listOf("§cTalk to \"Maxwell\"!")
    if (tunings.isEmpty()) return listOf("§cNo Maxwell Tunings :(")

    val title = pluralize(tunings.size, "Tuning", "Tunings")
    return if (maxwellConfig.compactTuning) {
        val tuning = tunings
            .take(3)
            .joinToString("§7, ") { tuning ->
                with(tuning) {
                    if (displayConfig.displayNumbersFirst) {
                        "$color$value$icon"
                    } else "$color$icon$value"
                }

            }
        listOf(
            (if (displayConfig.displayNumbersFirst) {
                "$tuning §f$title"
            } else "$title: $tuning"),
        )
    } else {
        val tuning = tunings
            .take(maxwellConfig.tuningAmount.coerceAtLeast(1))
            .map { tuning ->
                with(tuning) {
                    " §7- §f" + if (displayConfig.displayNumbersFirst) {
                        "$color$value $icon $name"
                    } else "$name: $color$value$icon"
                }
            }.toTypedArray()
        listOf("$title:", *tuning)
    }
}

private fun getPowerShowWhen() = !inAnyIsland(IslandType.THE_RIFT)

private fun getCookieDisplayPair() = listOf(
    "§dCookie Buff§f: " + (BitsAPI.cookieBuffTime?.let {
        if (!BitsAPI.hasCookieBuff()) "§cNot Active" else it.timeUntil().format(maxUnits = 2)
    } ?: "§cOpen SbMenu!"),
)

private fun getCookieShowWhen(): Boolean {
    if (HypixelData.bingo) return false
    return informationFilteringConfig.hideEmptyLines && BitsAPI.hasCookieBuff()
}

private fun getObjectiveDisplayPair() = buildList {
    val objective = SbPattern.objectivePattern.firstMatches(ScoreboardData.sidebarLinesFormatted) ?: return@buildList

    add(objective)
    add(ScoreboardData.sidebarLinesFormatted.nextAfter(objective) ?: HIDDEN)

    if (SbPattern.thirdObjectiveLinePattern.anyMatches(ScoreboardData.sidebarLinesFormatted)) {
        add(ScoreboardData.sidebarLinesFormatted.nextAfter(objective, 2) ?: "Second objective here")
    }
}

private fun getObjectiveShowWhen(): Boolean = SbPattern.objectivePattern.anyMatches(ScoreboardData.sidebarLinesFormatted)

private fun getSlayerDisplayPair() = buildList {
    add((if (SlayerAPI.hasActiveSlayerQuest()) "Slayer Quest" else HIDDEN))
    add(" §7- §e${SlayerAPI.latestSlayerCategory.trim()}")
    add(" §7- §e${SlayerAPI.latestSlayerProgress.trim()}")
}

private fun getSlayerShowWhen() = if (informationFilteringConfig.hideIrrelevantLines) SlayerAPI.isInCorrectArea else true

private fun getQuiverDisplayPair(): List<String> {
    if (QuiverAPI.currentArrow == null)
        return listOf("§cChange your Arrow once")
    if (QuiverAPI.currentArrow == NONE_ARROW_TYPE)
        return listOf("No Arrows selected")

    val amountString = (
        if (arrowConfig.colorArrowAmount) {
            percentageColor(
                QuiverAPI.currentAmount.toLong(),
                QuiverAPI.MAX_ARROW_AMOUNT.toLong(),
            ).getChatColor()
        } else "") +
        if (QuiverAPI.wearingSkeletonMasterChestplate) "∞"
        else {
            when (arrowConfig.arrowAmountDisplay) {
                ArrowAmountDisplay.NUMBER -> QuiverAPI.currentAmount.addSeparators()
                ArrowAmountDisplay.PERCENTAGE -> "${QuiverAPI.currentAmount.asArrowPercentage()}%"
                else -> QuiverAPI.currentAmount.addSeparators()
            }
        }

    return listOf(
        if (displayConfig.displayNumbersFirst) {
            "$amountString ${QuiverAPI.currentArrow?.arrow}s"
        } else {
            "Arrows: $amountString ${QuiverAPI.currentArrow?.arrow?.replace(" Arrow", "")}"
        },
    )
}

private fun getQuiverShowWhen(): Boolean {
    if (informationFilteringConfig.hideIrrelevantLines && !QuiverAPI.hasBowInInventory()) return false
    return !inAnyIsland(IslandType.THE_RIFT)
}

private fun getPowderDisplayPair() = buildList {
    val powderTypes = HotmAPI.Powder.entries
    if (informationFilteringConfig.hideEmptyLines && powderTypes.all { it.getTotal() == 0L }) {
        return@buildList
    }

    add("§9§lPowder")

    val displayNumbersFirst = displayConfig.displayNumbersFirst

    for (type in powderTypes) {
        val name = type.displayName
        val color = type.color
        val current = formatNumber(type.getCurrent())
        val total = formatNumber(type.getTotal())

        when (displayConfig.powderDisplay) {
            PowderDisplay.AVAILABLE -> {
                add(" §7- ${if (displayNumbersFirst) "$color$current $name" else "§f$name: $color$current"}")
            }

            PowderDisplay.TOTAL -> {
                add(" §7- ${if (displayNumbersFirst) "$color$total $name" else "§f$name: $color$total"}")
            }

            PowderDisplay.BOTH -> {
                add(" §7- ${if (displayNumbersFirst) "$color$current/$total $name" else "§f$name: $color$current/$total"}")
            }

            null -> {}
        }
    }
}

private fun getPowderShowWhen() = inAdvancedMiningIsland()

private fun getEventsDisplayPair(): List<ScoreboardElementType> {
    return ScoreboardEvents.getEvent()
        .filterNotNull()
        .flatMap { it.getLines() }
}

private fun getMayorDisplayPair() = buildList {
    val currentMayorName = MayorAPI.currentMayor?.mayorName?.let { MayorAPI.mayorNameWithColorCode(it) } ?: HIDDEN
    val timeTillNextMayor = if (mayorConfig.showTimeTillNextMayor) {
        "§7 (§e${MayorAPI.nextMayorTimestamp.timeUntil().format(maxUnits = 2)}§7)"
    } else ""

    add(currentMayorName + timeTillNextMayor)

    if (mayorConfig.showMayorPerks) {
        MayorAPI.currentMayor?.activePerks?.forEach { perk ->
            add(" §7- §e${perk.perkName}")
        }
    }

    if (!mayorConfig.showExtraMayor) return@buildList
    val jerryExtraMayor = MayorAPI.jerryExtraMayor
    val extraMayor = jerryExtraMayor.first ?: return@buildList

    val extraMayorName = extraMayor.mayorName.let { MayorAPI.mayorNameWithColorCode(it) }
    val extraTimeTillNextMayor = if (mayorConfig.showTimeTillNextMayor) {
        "§7 (§6${jerryExtraMayor.second.timeUntil().format(maxUnits = 2)}§7)"
    } else ""

    add(extraMayorName + extraTimeTillNextMayor)

}

private fun getMayorShowWhen() = !inAnyIsland(IslandType.THE_RIFT) && MayorAPI.currentMayor != null

private fun getPartyDisplayPair() =
    if (PartyAPI.partyMembers.isEmpty() && informationFilteringConfig.hideEmptyLines) listOf(HIDDEN)
    else {
        val title = if (PartyAPI.partyMembers.isEmpty()) "§9§lParty" else "§9§lParty (${PartyAPI.partyMembers.size})"
        val partyList = PartyAPI.partyMembers
            .take(partyConfig.maxPartyList.get())
            .map { " §7- §f$it" }
            .toTypedArray()
        listOf(title, *partyList)
    }

private fun getPartyShowWhen() = if (DungeonAPI.inDungeon()) {
    false // Hidden bc the scoreboard lines already exist
} else {
    if (partyConfig.showPartyEverywhere) {
        true
    } else {
        inAnyIsland(IslandType.DUNGEON_HUB, IslandType.KUUDRA_ARENA, IslandType.CRIMSON_ISLE) || inGlaciteArea()
    }
}

private fun getFooterDisplayPair(): List<ScoreboardElementType> = listOf(
    displayConfig.titleAndFooter.customFooter
        .replace("&", "§")
        .split("\\n")
        .map { it to displayConfig.titleAndFooter.alignTitleAndFooter },
).flatten()

private fun getExtraDisplayPair(): List<String> {
    if (unconfirmedUnknownLines.isEmpty()) return listOf(HIDDEN)
    amountOfUnknownLines = unconfirmedUnknownLines.size

    return listOf("§cUndetected Lines:") + unconfirmedUnknownLines
}

private fun getExtraShowWhen(): Boolean {
    if (unconfirmedUnknownLines.isEmpty()) {
        amountOfUnknownLines = 0
    }
    return unconfirmedUnknownLines.isNotEmpty()
}
