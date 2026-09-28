package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.WarpsJson
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object WarpApi {
    private val patternGroup = RepoPattern.group("warp-api")

    private val failedWarps: MutableSet<String> get() = ProfileStorageData.profileSpecific?.failedWarps ?: mutableSetOf()

    val travelScrollNotUnlockedPattern by patternGroup.pattern(
        "travel-scroll-not-unlocked",
        "You haven't unlocked this fast travel destination!",
    )

    val requirementWarpMissing by patternGroup.pattern(
        "requirement-warp-missing",
        "You don't have the requirements to use this warp!",
    )

    data class WarpLocation(
        val identifier: String,
        val displayName: String,
        val island: IslandType,
        val position: LorenzVec,
        val commands: List<String>,
    ) {
        val command: String get() = commands.first()
    }

    private var warps: Map<IslandType, List<WarpLocation>> = emptyMap()

    private data class ScheduledWarp(
        val position: LorenzVec,
        val island: IslandType,
        val shouldRetry: Boolean,
        val onWarp: () -> Unit,
        val onFail: () -> Unit,
        val onWarpFail: (WarpLocation, () -> Unit) -> Unit,
        var warp: WarpLocation,
    )
    private var scheduledWarp: ScheduledWarp? = null
    private var pendingWarp: WarpLocation? = null

    @HandleEvent
    private fun onIslandGraphReload() {
        val warp = pendingWarp ?: return
        if (SkyBlockUtils.currentIsland != warp.island) return
        failedWarps.remove(warp.identifier)
        pendingWarp = null

        scheduledWarp?.let {
            if (it.warp == warp) {
                it.onWarp.invoke()
                scheduledWarp = null
            }
        }
    }

    @HandleEvent
    private fun onRepoReload(event: RepositoryReloadEvent) {
        reset()
        val warpsJson = event.getConstant<WarpsJson>("Warps").warpLocation
        warps = warpsJson.map { (name, warp) ->
            val commands = warp.commands.orEmpty().takeIfNotEmpty() ?: listOf(name.lowercase())
            WarpLocation(
                identifier = name,
                displayName = warp.displayName,
                island = warp.island,
                position = LorenzVec(warp.x, warp.y, warp.z),
                commands = commands,
            )
        }.groupBy { it.island }
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onMessageSendToServer(event: MessageSendToServerEvent) {
        val args = event.message.lowercase().split(" ")
        if (args.size != 2) return
        if (args[0] != "/warp") return
        val warpName = args[1]
        val warp = warps.values.flatten().find { warpName in it.commands } ?: return

        pendingWarp = warp
    }

    @HandleEvent
    private fun onChat(event: SystemMessageEvent.Allow) {
        if (
            !travelScrollNotUnlockedPattern.matches(event.cleanMessage) &&
            !requirementWarpMissing.matches(event.cleanMessage)
        ) return

        val warp = pendingWarp ?: return
        failedWarps.add(warp.identifier)
        pendingWarp = null

        val scheduled = scheduledWarp ?: return
        if (scheduled.warp != warp) return

        val nextWarp = if (scheduled.shouldRetry) {
            getNearestWarp(scheduled.position, scheduled.island)
        } else {
            null
        }

        if (nextWarp == null) {
            scheduled.onFail()
            scheduledWarp = null
            return
        }

        scheduledWarp = null

        scheduled.onWarpFail(nextWarp) {
            val newScheduledWarp = scheduled.copy(warp = nextWarp)
            scheduledWarp = newScheduledWarp
            pendingWarp = newScheduledWarp.warp
            HypixelCommands.warp(newScheduledWarp.warp.command)
        }
    }

    @HandleEvent
    private fun onIslandLeave() {
        if (SkyBlockUtils.currentIsland != NONE) return
        reset()
    }

    @HandleEvent
    private fun onDisconnect() {
        reset()
    }

    @HandleEvent
    private fun onCommandRegister(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetfailedwarps") {
            description = "Resets the failed warps list"
            category = USERS_BUG_FIX
            simpleCallback {
                failedWarps.clear()
                ChatUtils.chat("Failed warps list cleared!")
            }
        }
    }

    fun sendWarpMessage(
        position: LorenzVec,
        island: IslandType = SkyBlockUtils.currentIsland,
        shouldRetry: Boolean = true,
        onWarp: () -> Unit = {},
        onWarpFail: (WarpLocation, () -> Unit) -> Unit = ::defaultWarpFailHandler,
        onFail: () -> Unit = {},
    ) {
        val warp = getNearestWarp(position, island) ?: run {
            onFail()
            return
        }

        ChatUtils.clickableChat(
            "§7Click §e§lHERE§r §7to warp there using §e/${warp.command}§7!",
            onClick = {
                scheduledWarp = ScheduledWarp(
                    position = position,
                    island = island,
                    shouldRetry = shouldRetry,
                    onWarp = onWarp,
                    onFail = onFail,
                    onWarpFail = onWarpFail,
                    warp = warp,
                )
                HypixelCommands.warp(warp.command)
            },
        )
    }

    fun defaultWarpFailHandler(
        nextWarp: WarpLocation,
        retry: () -> Unit,
    ) {
        // Ensure only shows the message after the warp fail message
        DelayedRun.runNextTickEnd {
            ChatUtils.clickableChat(
                "§7That warp failed. §7Click §e§lHERE§r §7to try §e/${nextWarp.command}§7 instead.",
                onClick = retry,
            )
        }
    }

    fun getNearestWarp(position: LorenzVec, island: IslandType = SkyBlockUtils.currentIsland): WarpLocation? {
        return warps[island]
            ?.filterNot { it.identifier in failedWarps }
            ?.minByOrNull { it.position.distanceSq(position) }
    }

    private fun reset() {
        scheduledWarp = null
        pendingWarp = null
    }
}
