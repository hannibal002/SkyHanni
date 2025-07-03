package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrEmpty
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.onClick
import at.hannibal2.skyhanni.utils.compat.hover
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object CompactSweepDetails {

    private val config get() = SkyHanniMod.feature.foraging.trees
    private val patternGroup = RepoPattern.group("foraging.sweep-details")

    /**
     * REGEX-TEST: §6Sweep Details: §r§2442∮ Sweep
     * REGEX-TEST: §6Sweep Details: §r§2442.76∮ Sweep
     * REGEX-TEST: §6Sweep Details: §r§2451.65∮ Sweep
     * REGEX-TEST: §6Sweep Details: §r§2430.65∮ Sweep
     * REGEX-TEST: §6Sweep Details: §r§234,442.2∮ Sweep
     */
    @Suppress("MaxLineLength")
    private val sweepDetailsPattern by patternGroup.pattern(
        "header",
        "(?:§.)+Sweep Details: (?<sweep>(?:§.)+(?<sweepAmt>[\\d,.]+)).? Sweep",
    )

    /**
     * REGEX-TEST:   §r§7Fig Tree Toughness: §r§63.5 §r§a18.13 Logs
     * REGEX-TEST:   §r§7Fig Tree Toughness: §r§63.5 §r§a18.19 Logs
     * REGEX-TEST:   §r§7Fig Tree Toughness: §r§63.5 §r§818.19 Logs
     * REGEX-TEST:   §r§7Fig Tree Toughness: §r§63.5 §r§818.04 Logs
     * REGEX-TEST:   §r§7Fig Tree Toughness: §r§63.5 §r§818 Logs
     * REGEX-TEST:   §r§7Dark Oak Tree Toughness: §r§60 §r§a35 Logs
     */
    @Suppress("MaxLineLength")
    private val sweepToughnessLogsPattern by patternGroup.pattern(
        "toughness-and-logs",
        "\\s+(?:§.)+(?<treeType>[\\S ]+) Tree Toughness: (?<toughnessDisplay>§r§6(?<toughnessAmount>[\\d,.]+)) (?<logsDisplay>(?:§.)+(?<isItGreen>§.)(?<logsAmount>[\\d,.]+)) Logs",
    )

    /**
     * REGEX-TEST:   §r§7Axe throw: §r§c-50% Sweep §r§a9.02 Logs
     * REGEX-TEST:   §r§7Axe throw: §r§c-50% Sweep §r§89.02 Logs
     * REGEX-TEST:   §r§7Wrong Style: §r§c-50% Sweep §r§a9.1 Logs §r§cCut the trunk first!!
     * REGEX-TEST:   §r§7Wrong Style: §r§c-50% Sweep §r§a4.51 Logs §r§cCut the trunk first!!
     * REGEX-TEST:   §r§7Wrong Style: §c-50% Sweep §a2.38 Logs §cCut branches and trunk first!!
     * REGEX-TEST:   §r§7Wrong Style: §r§c-50% Sweep §r§a2.38 Logs §r§cCut branches and trunk first!!
     */
    @Suppress("MaxLineLength")
    private val penaltyPattern by patternGroup.pattern(
        "penalty",
        "\\s+(?:§.)+(?<penaltyReason>[\\S ]+): (?<penaltyDisplay>(?:§.)+-(?<penaltyPercent>[\\d,.]+)%) Sweep (?<logsDisplay>(?:§.)?(?<isItGreen>§.)(?<logsAmount>[\\d,.]+)) Logs(?: (?<proTip>(?:§.)+[\\S ]+))?",
    )

    private var sweepDetailsVariablesDirty = false
    private var isInsideSweepDetails = false
    private var isFinalCalculation = false

    private var addedInitialLogs = false
    private var logCountDisplay = ""
    private var logs = -1.0
    private var proTip = ""
    private val sweepDetailsChatBreakdown = mutableListOf<String>()
    private var sweepDisplay = ""
    private val sweepPenaltyHoverHistory = mutableListOf<String>()
    private var sweep = -1.0
    private var toughness = -1.0
    private var toughnessDisplay = ""
    private var treeType = ""

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        if (!isInIsland() || !config.compactSweepDetails) return
        event.blockAndReadDetails()
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.oldIsland != IslandType.GALATEA) return
        resetSweepDetailsVariables()
    }

    private fun SkyHanniChatEvent.blockAndReadDetails() {
        val message = message
        sweepDetailsPattern.matchMatcher(message) {
            if (sweepPenaltyHoverHistory.isNotEmpty()) {
                // this flow needs to be here in case Axe Throw ability from HOTF screws up all the detection
                sendCompactedResults()
            }
            isInsideSweepDetails = true // always set this to true so future messages get blocked properly regardless of Axe Throw status
            sweepDetailsVariablesDirty =
                true // always set this to true so future messages get blocked properly regardless of Axe Throw status
            addedInitialLogs = false
            sweepDisplay = group("sweep")
            sweepPenaltyHoverHistory.add("§eClick to open the Tree Gifts guide!")
            sweepPenaltyHoverHistory.add("§6Initial §2Sweep§7: §2${group("sweepAmt").formatDouble()}")
            sweepDetailsChatBreakdown.add("§2Sweep: $sweepDisplay")
            blockedReason = "SWEEP_DETAILS"
            return
        }
        if (isInsideSweepDetails) {
            sweepToughnessLogsPattern.matchMatcher(message) {
                treeType = group("treeType")
                toughnessDisplay = group("toughnessDisplay")
                toughness = group("toughnessAmount").formatDouble()
                logCountDisplay = group("logsDisplay")
                logs = group("logsAmount").formatDouble()
                sweepPenaltyHoverHistory.add("§6Initial Logs: $logs $treeType Logs §7(§6$toughness toughness§7)")
                blockedReason = "SWEEP_DETAILS"
                if (isFinalCalculation(group("isItGreen")))
                    sendCompactedResults()
            }
            penaltyPattern.matchMatcher(message) {
                if (!addedInitialLogs) {
                    sweepDetailsChatBreakdown.add("§7, §e$logs logs")
                    addedInitialLogs = true
                }
                logs = group("logsAmount").formatDouble()
                logCountDisplay = group("logsDisplay")
                sweepPenaltyHoverHistory.add("§e${group("penaltyReason")}§7: §c-${group("penaltyPercent").formatDouble()}% §7($logs logs)")
                sweepDetailsChatBreakdown.add("§7(${group("penaltyDisplay")}§7)")
                proTip = groupOrEmpty("proTip")
                blockedReason = "SWEEP_DETAILS"
                if (isFinalCalculation(group("isItGreen")))
                    sendCompactedResults()
            }
        }
    }

    private fun sendCompactedResults() {
        sweepPenaltyHoverHistory.add("§6Final Logs: §a$logs §6$treeType Logs")
        isInsideSweepDetails = false

        val builder = StringBuilder()

        sweepPenaltyHoverHistory.forEach { penalty ->
            builder.append(penalty)
            if (penalty != sweepPenaltyHoverHistory.last()) builder.append("\n")
        }
        if (proTip.isNotEmpty()) builder.append("\n§6Pro tip: $proTip")
        val hoverText = builder.toString()
        val hoverComponent = hoverText.asComponent()

        builder.clear()

        sweepDetailsChatBreakdown.forEach { section ->
            builder.append(section)
            if (!section.startsWith("§2Sweep: ") || !addedInitialLogs) builder.append(" ")
        }
        builder.append("§7-> §a$logs logs")
        if (proTip.isNotEmpty()) builder.append("\n  §6Pro tip: $proTip")
        val chatText = builder.toString()
        val chatComponent = chatText.asComponent()

        chatComponent.hover = hoverComponent
        chatComponent.onClick(onClick = {
            HypixelCommands.treeGifts()
        })

        ChatUtils.chat(chatComponent)
        resetSweepDetailsVariables()
    }

    private fun isFinalCalculation(regexGroup: String): Boolean = regexGroup == "§a"

    private fun resetSweepDetailsVariables() {
        if (!sweepDetailsVariablesDirty) return

        sweepPenaltyHoverHistory.clear()
        isInsideSweepDetails = false
        isFinalCalculation = false

        addedInitialLogs = false
        logCountDisplay = ""
        logs = -1.0
        proTip = ""
        sweepDetailsChatBreakdown.clear()
        sweepDisplay = ""
        sweepPenaltyHoverHistory.clear()
        sweep = -1.0
        toughness = -1.0
        toughnessDisplay = ""
        treeType = ""

        sweepDetailsVariablesDirty = false
    }

    private fun isInIsland() =
        SkyBlockUtils.inSkyBlock && SkyBlockUtils.inAnyIsland(IslandType.THE_PARK, IslandType.GALATEA, IslandType.HUB)
}
