package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.model.graph.GraphNodeTag
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.misc.pathfind.NavigateAllHelper
import at.hannibal2.skyhanni.features.misc.pathfind.NavigationCondition
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object HoneyhiveReminder {

    private val config get() = SkyHanniMod.feature.foraging.honeyhive

    private val storage get() = ProfileStorageData.profileSpecific?.foraging

    private val patternGroup = RepoPattern.group("foraging.honeyhive")

    /**
     * REGEX-TEST: You stick your hand into the honeyhive and feel around...
     */
    private val hiveLootedPattern by patternGroup.pattern(
        "hive-looted",
        "You stick your hand into the honeyhive and feel around\\.\\.\\.",
    )

    /**
     * REGEX-TEST: QUEEN BEE! The Honeyhive instantly refilled with loot!
     */
    private val queenBeePattern by patternGroup.pattern(
        "queen-bee",
        "QUEEN BEE! The Honeyhive instantly refilled with loot!",
    )

    private var notifyOnIslandSwap = false

    @HandleEvent(onlyOnIsland = IslandType.TORRHUS_CANYON)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!config.queenBeeNotification) return
        if (!queenBeePattern.matches(event.cleanMessage)) return
        TitleManager.sendTitle("§6Honeyhive Instantly Refilled!")
    }

    @HandleEvent
    private fun onSecondPassed() {
        val storage = storage ?: return

        if (!config.enabled) return
        if (storage.honeyhiveRemindTime.isInFuture()) return

        if (NavigateAllHelper.currentlyNavigating) return

        if (IslandType.TORRHUS_CANYON.isInIsland()) {
            val graph = IslandGraphs.currentIslandGraph ?: return
            val nodes = graph.getNodesWithTags(GraphNodeTag.HONEYHIVE)

            ChatUtils.clickToActionOrDisable(
                "Honeyhives are ready to be collected.",
                config::enabled,
                actionName = "navigate to all Honeyhives",
                action = {
                    NavigateAllHelper.navigateAll(
                        nodes,
                        GraphNodeTag.HONEYHIVE.displayName,
                        LorenzColor.GOLD.toColor(),
                        onFinish = {
                            ChatUtils.chat("You visited all ${GraphNodeTag.HONEYHIVE.displayName}s")
                            storage.honeyhiveRemindTime = 1.hours.fromNow()
                        },
                        continueNavigationCondition = NavigationCondition.ChatMessage { message -> hiveLootedPattern.matches(message) },
                        condition = { config.enabled },
                    )
                },
            )
        } else if (config.reminderOutsideTorrhus) {
            ChatUtils.clickToActionOrDisable(
                "Honeyhives are ready to be collected on the Torrhus Canyon.",
                config::reminderOutsideTorrhus,
                actionName = "warp to the Torrhus Canyon",
                action = {
                    notifyOnIslandSwap = true
                    HypixelCommands.warp("torrhus")
                },
            )
        } else {
            return
        }
        storage.honeyhiveRemindTime = 5.minutes.fromNow()
    }

    @HandleEvent(onlyOnIsland = IslandType.TORRHUS_CANYON)
    private fun onIslandJoin() {
        if (!notifyOnIslandSwap) return
        notifyOnIslandSwap = false

        DelayedRun.runDelayed(3.seconds) {
            storage?.honeyhiveRemindTime = SimpleTimeMark.farPast()
        }
    }

}
