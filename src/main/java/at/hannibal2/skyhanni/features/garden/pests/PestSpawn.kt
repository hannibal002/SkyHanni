package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.garden.pests.PestSpawnConfig
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.garden.pests.PestSpawnEvent
import at.hannibal2.skyhanni.features.garden.pests.PestApi.lastPestSpawnTime
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatchers
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PestSpawn {

    private val config get() = PestApi.config.pestSpawn

    private val patternGroup = RepoPattern.group("garden.pests.spawn")

    /**
     * REGEX-TEST: GROSS! A ൠ Pest has appeared in Plot - S 4!
     * REGEX-TEST: GROSS! A ൠ Pest has appeared in The Barn!
     * REGEX-FAIL: From [MVP+] ThePleader: GROSS! A ൠ Pest has appeared in Plot - 67!
     */
    private val onePestPattern by patternGroup.list(
        "one.colorless",
        "^\\w+! A ൠ Pest has appeared in Plot - (?<plot>.*)!",
        "^\\w+! A ൠ Pest has appeared in (?<plot>The Barn)!",
    )

    /**
     * REGEX-TEST: YUCK! 4 ൠ Pest have spawned in Plot - 14!
     * REGEX-TEST: YUCK! 4 ൠ Pest have spawned in The Barn!
     * REGEX-FAIL: From [MVP+] ThePleader: YUCK! 6 ൠ Pest have spawned in Plot - 7!
     */
    private val multiplePestsPattern by patternGroup.list(
        "multiple.colorless",
        "^\\w+! (?<amount>\\d) ൠ Pests? have spawned in Plot - (?<plot>.*)!",
        "^\\w+! (?<amount>\\d) ൠ Pests? have spawned in (?<plot>The Barn)!",
    )

    /**
     * REGEX-TEST: GROSS! While you were offline, ൠ Pest spawned in Plots 12, 9, 5, 11 and 3!
     * REGEX-FAIL: From [MVP+] ThePleader: GROSS! While you were offline, ൠ Pest spawned in Plots 6 and 7!
     */
    private val offlinePestsPattern by patternGroup.pattern(
        "offline.colorless",
        "^\\w+! While you were offline, ൠ Pests? spawned in Plots (?<plots>.*)!",
    )

    /**
     * WRAPPED-REGEX-TEST: "  CLICK HERE to teleport to the plot!"
     */
    private val clickToTPPattern by patternGroup.pattern(
        "teleport.colorless",
        "\\s*CLICK HERE to teleport to the plot!",
    )

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onChat(event: SystemMessageEvent.Allow) {
        val message = event.cleanMessage
        var blocked = false

        onePestPattern.matchMatchers(message) {
            spawn(1, listOf(group("plot")))
            blocked = true
        }
        multiplePestsPattern.matchMatchers(message) {
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
            val tpName = if (plotName == "The Barn") "barn" else plotName
            ChatUtils.clickableChat(
                message,
                onClick = {
                    HypixelCommands.teleportToPlot(tpName)
                },
                "§eClick to run /plottp $tpName!",
            )
        }
    }
}
