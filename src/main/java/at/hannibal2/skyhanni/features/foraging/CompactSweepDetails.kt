package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.storage.ResettableStorageSet
import at.hannibal2.skyhanni.data.IslandTypeTags
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrEmpty
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
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

    data class SweepDetails(
        var addedInitialLogs: Boolean = false,
        var logCountDisplay: String = "",
        var logs: Double = -1.0,
        var proTip: String = "",
        val breakdown: MutableList<String> = mutableListOf(),
        val penalties: MutableList<String> = mutableListOf(),
        var sweepDisplay: String = "",
        var sweep: Double = -1.0,
        var toughness: Double = -1.0,
        var treeType: String = "",
    ) : ResettableStorageSet()

    private var sweepDetailsAreDirty = false
    private var isInsideSweepDetails = false
    private var sweepDetails: SweepDetails = SweepDetails()

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        if (!isInIsland() || !config.compactSweepDetails) return
        sweepDetailsPattern.matchMatcher(event.message) {
            if (sweepDetails.penalties.isNotEmpty()) {
                sendCompactedResults()
            }
            // Set these to true so future messages get blocked properly regardless of Axe Throw status
            isInsideSweepDetails = true
            sweepDetailsAreDirty = true
            sweepDetails = SweepDetails()
            sweepDetails.addedInitialLogs = false
            sweepDetails.sweepDisplay = group("sweep")
            sweepDetails.penalties.addAll(
                listOf(
                    "§eClick to open the Tree Gifts guide!",
                    "§6Initial §2Sweep§7: §2${group("sweepAmt").formatDouble()}",
                ),
            )
            sweepDetails.breakdown.add("§2Sweep: ${sweepDetails.sweepDisplay}")
            event.blockedReason = "SWEEP_DETAILS"
            return
        }
        if (!isInsideSweepDetails) return
        sweepToughnessLogsPattern.matchMatcher(event.message) {
            val toughnessAmount = group("toughnessAmount")
            val fixedToughness = toughnessAmount.removeSuffix(".0")
            sweepDetails.treeType = group("treeType")
            sweepDetails.toughness = toughnessAmount.formatDouble()
            sweepDetails.logCountDisplay = group("logsDisplay")
            sweepDetails.logs = group("logsAmount").formatDouble()
            sweepDetails.penalties.add(
                "§6Initial Logs: ${sweepDetails.logs} " +
                    "${sweepDetails.treeType} Logs §7(§6$fixedToughness toughness§7)",
            )
            event.blockedReason = "SWEEP_DETAILS"
            if (isFinalCalculation(group("isItGreen"))) {
                sendCompactedResults()
            }
        }
        penaltyPattern.matchMatcher(event.message) {
            if (!sweepDetails.addedInitialLogs) {
                sweepDetails.breakdown.add("§7, §e${sweepDetails.logs} logs")
                sweepDetails.addedInitialLogs = true
            }
            sweepDetails.logs = group("logsAmount").formatDouble()
            sweepDetails.logCountDisplay = group("logsDisplay")
            sweepDetails.penalties.add(
                "§e${group("penaltyReason")}§7: " +
                    "§c-${group("penaltyPercent").formatDouble()}% " +
                    "§7(${sweepDetails.logs} logs)",
            )
            sweepDetails.breakdown.add("§7(${group("penaltyDisplay")}§7)")
            sweepDetails.proTip = groupOrEmpty("proTip")
            event.blockedReason = "SWEEP_DETAILS"
            if (isFinalCalculation(group("isItGreen"))) {
                sendCompactedResults()
            }
        }
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        resetSweepDetailsVariables()
    }

    private fun sendCompactedResults() {
        sweepDetails.penalties.add(
            "§6Final Logs: §a${sweepDetails.logs} " +
                "§6${sweepDetails.treeType} Logs",
        )
        isInsideSweepDetails = false

        val builder = StringBuilder()

        sweepDetails.penalties.forEach { penalty ->
            builder.append(penalty)
            if (penalty != sweepDetails.penalties.last()) builder.append("\n")
        }
        if (sweepDetails.proTip.isNotEmpty()) {
            builder.append("\n§6Pro tip: ${sweepDetails.proTip}")
        }
        val hoverText = builder.toString()

        builder.clear()

        sweepDetails.breakdown.forEach { section ->
            builder.append(section)
            if (!section.startsWith("§2Sweep: ") || !sweepDetails.addedInitialLogs) {
                builder.append(" ")
            }
        }
        builder.append("§7-> §a${sweepDetails.logs} logs")
        if (sweepDetails.proTip.isNotEmpty()) {
            builder.append("\n  §6Pro tip: ${sweepDetails.proTip}")
        }
        val chatText = builder.toString()
        val chatComponent = chatText.asComponent()

        chatComponent.hover = hoverText.asComponent()
        chatComponent.onClick(onClick = {
            HypixelCommands.treeGifts()
        })

        ChatUtils.chat(chatComponent)
        resetSweepDetailsVariables()
    }

    private fun resetSweepDetailsVariables() {
        if (!sweepDetailsAreDirty) return

        isInsideSweepDetails = false

        sweepDetails.reset()

        sweepDetailsAreDirty = false
    }

    private fun isFinalCalculation(regexGroup: String): Boolean = regexGroup == "§a"

    private fun isInIsland(): Boolean = IslandTypeTags.FORAGING.inAny()
}
