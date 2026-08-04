package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.model.graph.GraphNodeTag
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.fame.ReminderUtils
import at.hannibal2.skyhanni.features.misc.pathfind.NavigateAllHelper
import at.hannibal2.skyhanni.features.misc.pathfind.NavigationCondition
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object HoneyhiveReminder {

    private val config get() = SkyHanniMod.feature.foraging.honeyhiveReminder

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
    private var currentlyNavigating = false

    @HandleEvent(onlyOnIsland = IslandType.TORRHUS_CANYON)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!config.enabled) return

        if (!currentlyNavigating && hiveLootedPattern.matches(event.cleanMessage)) {
            val location = LocationUtils.playerLocation()

            startHiveNavigation("You looted a honeyhive, want to collect the rest?", location)

            return
        }

        if (!config.queenBeeNotification) return
        if (!queenBeePattern.matches(event.cleanMessage)) return
        TitleManager.sendTitle("§6Honeyhive Instantly Refilled!")
    }

    @HandleEvent
    private fun onSecondPassed() {
        val storage = storage ?: return

        if (!config.enabled) return
        if (storage.honeyhiveRemindTime.isInFuture()) return

        if (currentlyNavigating || ReminderUtils.isBusy()) return

        if (IslandType.TORRHUS_CANYON.isInIsland()) {
            startHiveNavigation("Honeyhives are ready to be collected.")
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

    // If location is not null then we are excluding the closest hive to that location
    private fun startHiveNavigation(startMessage: String, location: LorenzVec? = null) {
        val storage = storage ?: return

        val graph = IslandGraphs.currentIslandGraph ?: return
        val nodes = graph.getNodesWithTags(GraphNodeTag.HONEYHIVE).toMutableList()

        if (nodes.isEmpty()) return

        location?.let { location ->
            val closest = nodes.minBy { it.position.distance(location) }
            nodes.remove(closest)
        }

        if (nodes.isEmpty()) return

        currentlyNavigating = true
        ChatUtils.clickToActionOrDisable(
            startMessage,
            config::enabled,
            actionName = "navigate to all Honeyhives",
            action = {
                NavigateAllHelper.navigateAll(
                    nodes,
                    GraphNodeTag.HONEYHIVE.displayName,
                    GraphNodeTag.HONEYHIVE.color.toColor(),
                    onFinish = {
                        ChatUtils.chat("You visited all ${StringUtils.pluralize(nodes.size, GraphNodeTag.HONEYHIVE.displayName)}§e.")
                        storage.honeyhiveRemindTime = 1.hours.fromNow()
                        currentlyNavigating = false
                    },
                    continueNavigationCondition = NavigationCondition.ChatMessage { message -> hiveLootedPattern.matches(message) },
                    condition = { config.enabled },
                )
            },
        )
    }

    @HandleEvent
    private fun onIslandLeave() {
        currentlyNavigating = false
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
