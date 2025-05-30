package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.repo.RepoManager
import at.hannibal2.skyhanni.data.repo.RepoManager.hasDefaultSettings
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
//#if TODO
import at.hannibal2.skyhanni.features.misc.CurrentPing
//#endif
import at.hannibal2.skyhanni.features.misc.TpsCounter
//#if TODO
import at.hannibal2.skyhanni.features.misc.limbo.LimboTimeTracker
//#endif
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.equalsIgnoreColor
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import at.hannibal2.skyhanni.utils.toLorenzVec
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// todo 1.21 impl needed
@SkyHanniModule
object DebugCommand {

    fun command(search: String) {
        val list = mutableListOf<String>()
        list.add("```")
        list.add("= Debug Information for SkyHanni ${SkyHanniMod.VERSION} ${PlatformUtils.MC_VERSION} =")
        list.add("")

        list.add(
            if (search.isNotEmpty()) {
                if (search.equalsIgnoreColor("all")) {
                    "search for everything:"
                } else "search '$search':"
            } else "no search specified, only showing interesting stuff:",
        )

        val event = DebugDataCollectEvent(list, search)

        // calling default debug stuff
        player(event)
        repoData(event)
        globalRender(event)
        skyblockStatus(event)
        networkInfo(event)
        profileName(event)
        profileType(event)

        event.post()

        if (event.empty) {
            list.add("")
            list.add("Nothing interesting to show right now!")
            list.add("Looking for something specific? /shdebug <search>")
            list.add("Wanna see everything? /shdebug all")
        }

        list.add("```")
        OSUtils.copyToClipboard(list.joinToString("\n"))
        ChatUtils.chat("§eCopied SkyHanni debug data in the clipboard.")
    }

    private fun profileType(event: DebugDataCollectEvent) {
        event.title("Profile Type")
        if (!SkyBlockUtils.inSkyBlock) {
            event.addIrrelevant("Not on SkyBlock")
            return
        }

        if (ProfileStorageData.playerSpecific == null) {
            event.addData("playerSpecific is null!")
            return
        }

        val classic = !SkyBlockUtils.noTradeMode
        if (classic) {
            event.addIrrelevant("on classic")
        } else {
            if (HypixelData.ironman) {
                event.addData("on ironman")
            }
            if (HypixelData.stranded) {
                event.addData("on stranded")
            }
            if (HypixelData.bingo) {
                event.addData("on bingo")
            }
        }
    }

    private fun profileName(event: DebugDataCollectEvent) {
        event.title("Profile Name")
        if (!SkyBlockUtils.inSkyBlock) {
            event.addIrrelevant("Not on SkyBlock")
            return
        }

        if (HypixelData.profileName != "") {
            event.addIrrelevant("profileName: '${HypixelData.profileName}'")
        } else {
            event.addData("profile name is empty!")
        }
    }

    private fun skyblockStatus(event: DebugDataCollectEvent) {
        event.title("SkyBlock Status")
        if (!SkyBlockUtils.onHypixel) {
            event.addData("not on Hypixel")
            return
        }
        if (!SkyBlockUtils.inSkyBlock) {
            event.addData("not on SkyBlock, but on Hypixel")
            return
        }
        if (SkyBlockUtils.currentIsland == IslandType.UNKNOWN) {
            event.addData("Unknown SkyBlock island!")
            return
        }
        if (SkyBlockUtils.currentIsland == IslandType.NONE) {
            event.addData("No SkyBlock island found!")
            return
        }

        if (SkyBlockUtils.currentIsland != HypixelData.skyBlockIsland) {
            event.addData {
                add("using a test island!")
                add("test island: ${SkyBlockIslandTest.testIsland}")
                add("real island: ${HypixelData.skyBlockIsland}")
            }
            return
        }

        event.addIrrelevant {
            add("on Hypixel SkyBlock")
            add("skyBlockIsland: ${SkyBlockUtils.currentIsland}")
            add("skyBlockArea:")
            //#if TODO
            add("  scoreboard: '${SkyBlockUtils.graphArea}'")
            add("  graph network: '${SkyBlockUtils.graphArea}'")
            //#endif
            with(MinecraftCompat.localPlayer.position.toLorenzVec().roundTo(1)) {
                add(" /shtestwaypoint $x $y $z pathfind")
            }
            add("isOnAlphaServer: '${SkyBlockUtils.isOnAlphaServer}'")
        }
    }

    private fun globalRender(event: DebugDataCollectEvent) {
        //#if TODO
        event.title("Global Render")
        if (SkyHanniDebugsAndTests.globalRender) {
            event.addIrrelevant("normal enabled")
        } else {
            event.addData {
                add("Global renderer is disabled!")
                add("No renderable elements from SkyHanni will show up anywhere!")
            }
        }
        //#endif
    }

    private fun repoData(event: DebugDataCollectEvent) {
        event.title("Repo Information")
        val config = SkyHanniMod.feature.dev.repo

        val hasDefaultSettings = config.location.hasDefaultSettings()
        val list = buildList {
            add(" repoAutoUpdate: ${config.repoAutoUpdate}")
            add(" usingBackupRepo: ${RepoManager.usingBackupRepo}")
            if (hasDefaultSettings) {
                add((" repo location: default"))
            } else {
                add(" non-default repo location: '${RepoManager.getRepoLocation()}'")
            }

            if (RepoManager.unsuccessfulConstants.isNotEmpty()) {
                add(" unsuccessful constants:")
                for (constant in RepoManager.unsuccessfulConstants) {
                    add("  - $constant")
                }
            }

            add(" loaded neu items: ${NeuItems.allNeuRepoItems().size}")
        }

        val isRelevant = RepoManager.usingBackupRepo || RepoManager.unsuccessfulConstants.isNotEmpty() || !hasDefaultSettings
        if (isRelevant) {
            event.addData(list)
        } else {
            event.addIrrelevant(list)
        }
    }

    private fun player(event: DebugDataCollectEvent) {
        event.title("Player")
        event.addIrrelevant {
            add("name: '${PlayerUtils.getName()}'")
            add("uuid: '${PlayerUtils.getUuid()}'")
        }
    }

    private const val TPS_LIMIT = 15.0
    private val pingLimit = 1.5.seconds

    private fun networkInfo(event: DebugDataCollectEvent) {
        //#if TODO
        event.title("Network Information")
        val tps = TpsCounter.tps ?: 0.0
        val pingEnabled = SkyHanniMod.feature.dev.hypixelPingApi

        val list = buildList {
            add("tps: $tps")
            add("ping: ${CurrentPing.averagePing.inWholeMilliseconds.formatTime()}")

            val lastWorldSwitch = SkyBlockUtils.lastWorldSwitch.passedSince()
            var showPreviousPings = CurrentPing.averagePing > pingLimit
            if (!pingEnabled) {
                add("Hypixel Ping Packet disabled in settings!")
                showPreviousPings = true
            }
            if (lastWorldSwitch < 1.minutes) {
                add("last world switch: ${lastWorldSwitch.format()} ago")
                showPreviousPings = true
            }
            if (CurrentPing.previousPings.any { it > 5_000 }) {
                showPreviousPings = true
            }
            if (showPreviousPings) {
                add("previousPings: ${CurrentPing.previousPings.map { it.formatTime() }}")
            }

            if (LimboTimeTracker.inLimbo) {
                add("currently in limbo!")
            }
        }



        if (tps < TPS_LIMIT || CurrentPing.averagePing > pingLimit || !pingEnabled) {
            event.addData(list)
        } else {
            event.addIrrelevant(list)
        }
        //#endif
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        // todo convert to actual brigadier
        event.registerBrigadier("shdebug") {
            description = "Copies SkyHanni debug data in the clipboard."
            category = CommandCategory.DEVELOPER_DEBUG
            argCallback("profilename profile", BrigadierArguments.string()) { profile ->
                HypixelData.profileName = profile.lowercase()
                ChatUtils.chat("§eManually set profileName to '${HypixelData.profileName}'")
            }
            literalCallback("all") {
                command("all")
            }
            argCallback("search", BrigadierArguments.greedyString()) { search ->
                command(search)
            }
            simpleCallback { command("") }
        }
    }

    private fun Long.formatTime(): String = if (this > 999) {
        this.milliseconds.format(showMilliSeconds = true)
    } else this.addSeparators() + "ms"
}
