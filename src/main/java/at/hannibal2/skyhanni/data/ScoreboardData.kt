package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.RawScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.ScoreboardTitleUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.features.inventory.FixIronman
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.lastColorCode
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.getPlayerNames
import at.hannibal2.skyhanni.utils.compat.getSidebarObjective
import net.minecraft.network.play.server.S3BPacketScoreboardObjective
import net.minecraft.network.play.server.S3CPacketUpdateScore
import net.minecraft.network.play.server.S3EPacketTeams
import net.minecraft.scoreboard.IScoreObjectiveCriteria
import net.minecraft.scoreboard.ScorePlayerTeam
//#if MC > 1.21
//$$ import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
//#endif

@SkyHanniModule
object ScoreboardData {

    var sidebarLinesFormatted: List<String> = emptyList()

    private var sidebarLines: List<String> = emptyList() // TODO rename to raw
    var sidebarLinesRaw: List<String> = emptyList() // TODO delete
    val objectiveTitle: String
        get() =
            MinecraftCompat.localWorldOrNull?.scoreboard?.getSidebarObjective()?.displayName.orEmpty()

    private var dirty = false

    private fun formatLines(rawList: List<String>) = buildList {
        for (line in rawList) {
            val separator = splitIcons.find { line.contains(it) } ?: continue
            val split = line.split(separator)
            val start = split[0]
            var end = if (split.size > 1) split[1] else ""

            /**
             * If the line is split into two parts, we need to remove the color code prefixes from the end part
             * to prevent the color from being applied to the start of `end`, which would cause the color to be
             * duplicated in the final output.
             *
             * This fucks up different Regex checks if not working correctly, like here:
             * ```
             * Pattern: '§8- (§.)+[\w\s]+Dragon§a [\w,.]+§.❤'
             * Lines: - '§8- §c§aApex Dra§agon§a 486M§c❤'
             *        - '§8- §c§6Flame Dr§6agon§a 460M§c❤'
             * ```
             */
            val lastColor = start.lastColorCode().orEmpty()

            // Generate the list of color suffixes
            val colorSuffixes = lastColor.chunked(2).toMutableList()

            // Iterate through the colorSuffixes to remove matching prefixes from 'end'
            for (suffix in colorSuffixes.toList()) {
                if (end.startsWith(suffix)) {
                    end = end.removePrefix(suffix)
                    colorSuffixes.remove(suffix)
                }
            }

            add(start + end)
        }
    }

    @HandleEvent(receiveCancelled = true)
    fun onPacketReceive(event: PacketReceivedEvent) {
        when (val packet = event.packet) {
            is S3CPacketUpdateScore -> {
                if (packet.objectiveName == "update") {
                    dirty = true
                }
            }

            is S3EPacketTeams -> {
                if (packet.name.startsWith("team_")) {
                    dirty = true
                }
            }

            is S3BPacketScoreboardObjective -> {
                val type = packet.func_179817_d()
                if (type != IScoreObjectiveCriteria.EnumRenderType.INTEGER) return
                val objectiveName = packet.func_149339_c()
                if (objectiveName == "health") return
                val objectiveValue = packet.func_149337_d()
                ScoreboardTitleUpdateEvent(objectiveValue, objectiveName).post()
            }
        }
    }

    private var monitor = false
    private var lastMonitorState = emptyList<String>()
    private var lastChangeTime = SimpleTimeMark.farPast()

    private fun monitor() {
        if (!monitor) return
        val currentList = fetchScoreboardLines()
        if (lastMonitorState != currentList) {
            val time = lastChangeTime.passedSince()
            lastChangeTime = SimpleTimeMark.now()
            println("Scoreboard Monitor: (new change after ${time.format(showMilliSeconds = true)})")
            for (s in currentList) {
                println("'$s'")
            }
        }
        lastMonitorState = currentList
        println(" ")
    }

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onTick() {
        if (!dirty) return
        dirty = false
        monitor()

        val list = fetchScoreboardLines().reversed()
        val semiFormatted = list.map { cleanSB(it) }
        if (semiFormatted != sidebarLines) {
            sidebarLines = semiFormatted
            RawScoreboardUpdateEvent(semiFormatted).post()
        }

        sidebarLinesRaw = list
        val new = formatLines(list)
        if (new != sidebarLinesFormatted) {
            val old = sidebarLinesFormatted
            sidebarLinesFormatted = new
            ScoreboardUpdateEvent(new, old).post()
        }
    }

    private fun cleanSB(scoreboard: String) = scoreboard.toCharArray().filter {
        // 10735 = Rift Blood Effigies symbol
        it.code in 21..126 || it.code == 167 || it.code == 10735
    }.joinToString(separator = "")

    private fun fetchScoreboardLines(): List<String> {
        val scoreboard = MinecraftCompat.localWorldOrNull?.scoreboard ?: return emptyList()
        val objective = scoreboard.getSidebarObjective() ?: return emptyList()
        var scores = scoreboard.getSortedScores(objective)
        val list = scores.getPlayerNames(scoreboard)
        //#if MC < 1.21
        scores = if (list.size > 15) {
            list.drop(15)
        } else {
            list
        }
        return scores.map {
            ScorePlayerTeam.formatPlayerName(scoreboard.getPlayersTeam(it.playerName), it.playerName)
        }
        //#else
        //$$ return list.map { it.formattedTextCompatLessResets() }
        //#endif
    }

    /**
     * Tries to replace a scoreboard line with a modified one
     * @param text The line to check and possibly replace
     * @return The replaced line, or null if it should be hidden
     */
    fun tryToReplaceScoreboardLine(text: String): String? {
        try {
            return tryToReplaceScoreboardLineHarder(text)
        } catch (t: Throwable) {
            ErrorManager.logErrorWithData(
                t,
                "Error while changing the scoreboard text.",
                "text" to text,
            )
            return text
        }
    }

    private fun tryToReplaceScoreboardLineHarder(text: String): String? {
        if (SkyHanniMod.feature.misc.hideScoreboardNumbers && text.startsWith("§c") && text.length <= 4) {
            return null
        }
        if (SkyHanniMod.feature.misc.hidePiggyScoreboard) {
            PurseApi.piggyPattern.matchMatcher(text) {
                val coins = group("coins")
                return "Purse: $coins"
            }
        }

        if (SkyHanniMod.feature.misc.colorMonthNames) {
            for (season in Season.entries) {
                if (text.trim().startsWith(season.prefix)) {
                    return season.colorCode + text
                }
            }
        }
        FixIronman.fixScoreboard(text)?.let {
            return it
        }

        return text
    }

    enum class Season(val prefix: String, val colorCode: String) {
        EARLY_SPRING("Early Spring", "§d"),
        SPRING("Spring", "§d"),
        LATE_SPRING("Late Spring", "§d"),
        EARLY_SUMMER("Early Summer", "§6"),
        SUMMER("Summer", "§6"),
        LATE_SUMMER("Late Summer", "§6"),
        EARLY_AUTUMN("Early Autumn", "§e"),
        AUTUMN("Autumn", "§e"),
        LATE_AUTUMN("Late Autumn", "§e"),
        EARLY_WINTER("Early Winter", "§9"),
        WINTER("Winter", "§9"),
        LATE_WINTER("Late Winter", "§9")
    }

    // TODO USE SH-REPO
    private val splitIcons = listOf(
        "\uD83C\uDF6B",
        "\uD83D\uDCA3",
        "\uD83D\uDC7D",
        "\uD83D\uDD2E",
        "\uD83D\uDC0D",
        "\uD83D\uDC7E",
        "\uD83C\uDF20",
        "\uD83C\uDF6D",
        "⚽",
        "\uD83C\uDFC0",
        "\uD83D\uDC79",
        "\uD83C\uDF81",
        "\uD83C\uDF89",
        "\uD83C\uDF82",
        "\uD83D\uDD2B",
    )

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shdebugscoreboard") {
            description =
                "Monitors the scoreboard changes: " +
                "Prints the raw scoreboard lines in the console after each update, with time since last update."
            category = CommandCategory.DEVELOPER_DEBUG
            callback {
                monitor = !monitor
                val action = if (monitor) "Enabled" else "Disabled"
                ChatUtils.chat("$action scoreboard monitoring in the console.")
            }
        }
    }
}
