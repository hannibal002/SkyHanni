package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.garden.pests.PestSpawnConfig
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.garden.pests.PestSpawnEvent
import at.hannibal2.skyhanni.features.garden.pests.PestApi.lastPestSpawnTime
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PestSpawn {

    private val config get() = PestApi.config.pestSpawn

    private val patternGroup = RepoPattern.group("garden.pests.spawn")

    /**
     * REGEX-TEST: §6§lGROSS! §7A §2ൠ Pest §7has appeared in §aPlot §7- §bS 4§7!
     */
    private val onePestPattern by patternGroup.pattern(
        "one",
        "§6§l.*! §7A §2ൠ Pest §7has appeared in §aPlot §7- §b(?<plot>.*)§7!",
    )

    /**
     * REGEX-TEST: §6§lYUCK! §24 §2ൠ Pest §7have spawned in §aPlot §7- §b14§7!
     */
    private val multiplePestsPattern by patternGroup.pattern(
        "multiple",
        "§6§l.*! §2(?<amount>\\d) §2ൠ Pests? §7have spawned in §aPlot §7- §b(?<plot>.*)§7!",
    )

    /**
     * REGEX-TEST: §6§lGROSS! §7While you were offline, §2ൠ §2Pest §7spawned in §aPlots §r§b12§r§7, §r§b9§r§7, §r§b5§r§7, §r§b11§r§7 and §r§b3§r§r§7!
     */
    private val offlinePestsPattern by patternGroup.pattern(
        "offline",
        "§6§l.*! §7While you were offline, §2ൠ §2Pests? §7spawned in §aPlots (?<plots>.*)!",
    )
    /**
     * WRAPPED-REGEX-TEST: "  §r§e§lCLICK HERE §eto teleport to the plot!"
     */
    private val clickToTPPattern by patternGroup.pattern(
        "teleport",
        "\\s*§r§e§lCLICK HERE §eto teleport to the plot!",
    )

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        val message = event.message
        var blocked = false

        onePestPattern.matchMatcher(message) {
            spawn(1, listOf(group("plot")))
            blocked = true
        }
        multiplePestsPattern.matchMatcher(message) {
            spawn(group("amount").toInt(), listOf(group("plot")))
            blocked = true
        }
        offlinePestsPattern.matchMatcher(message) {
            spawn(null, group("plots").removeColor().split(", ", " and ").toList())
            // blocked = true
        }

        clickToTPPattern.matchMatcher(message) {
            if (lastPestSpawnTime.passedSince() < 1.seconds) {
                blocked = true
            }
        }

        if (blocked && config.chatMessageFormat != PestSpawnConfig.ChatMessageFormatEntry.HYPIXEL) {
            event.blockedReason = "pests_spawn"
        }
    }

    private fun spawn(amount: Int?, plotNames: List<String>) {
        PestSpawnEvent(amount, plotNames).post()

        if (amount == null) return // TODO make this work with offline pest spawn messages
        val plotName = plotNames.firstOrNull() ?: error("first plot name is null")
        val pestName = StringUtils.pluralize(amount, "Pest")
        val message = "§e$amount §a$pestName Spawned in §b$plotName§a!"

        if (config.showTitle) {
            TitleManager.sendTitle(message, duration = 7.seconds)
        }

        if (config.chatMessageFormat == PestSpawnConfig.ChatMessageFormatEntry.COMPACT) {
            ChatUtils.clickableChat(
                message,
                onClick = {
                    HypixelCommands.teleportToPlot(plotName)
                },
                "§eClick to run /plottp $plotName!",
            )
        }
    }
}
