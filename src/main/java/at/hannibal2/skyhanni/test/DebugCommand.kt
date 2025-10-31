package at.hannibal2.hanni.test

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.enoughupdates.EnoughUpdatesRepoManager
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.commands.CommandCategory
import at.hannibal2.hanni.config.commands.CommandRegistrationEvent
import at.hannibal2.hanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.hanni.data.HypixelData
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.ProfileStorageData
import at.hannibal2.hanni.data.repo.HanniRepoManager
import at.hannibal2.hanni.events.DebugDataCollectEvent
import at.hannibal2.hanni.features.misc.CurrentPing
import at.hannibal2.hanni.features.misc.TpsCounter
import at.hannibal2.hanni.features.misc.limbo.LimboTimeTracker
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.NeuItems
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.OSUtils
import at.hannibal2.hanni.utils.PlayerUtils
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.StringUtils.equalsIgnoreColor
import at.hannibal2.hanni.utils.TimeUtils.format
import at.hannibal2.hanni.utils.compat.MinecraftCompat
import at.hannibal2.hanni.utils.system.PlatformUtils
import at.hannibal2.hanni.utils.toLorenzVec
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@HanniModule
object DebugCommand {

    fun command(search: String) {
        val list = mutableListOf<String>()
        list.add("```")
        list.add("= Debug Information for Hanni ${HanniMod.VERSION} ${PlatformUtils.MC_VERSION} =")
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
        ChatUtils.chat("§eCopied Hanni debug data in the clipboard.")
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
            add("  scoreboard: '${SkyBlockUtils.scoreboardArea}'")
            add("  graph network: '${SkyBlockUtils.graphArea}'")
            with(MinecraftCompat.localPlayer.position.toLorenzVec().roundTo(1)) {
                add(" /shtestwaypoint $x $y $z pathfind")
            }
            add("isOnAlphaServer: '${SkyBlockUtils.isOnAlphaServer}'")
        }
    }

    // todo clean this up so that it commonly reports on any AbstractRepoManager
    private fun repoData(event: DebugDataCollectEvent) {
        event.title("Repo Information")
        val config = HanniMod.feature.dev.repo

        val hasDefaultSettings = config.location.hasDefaultSettings()
        val unsuccessfulConstants = HanniRepoManager.getFailedConstants()
        val list = buildList {
            add(" repoAutoUpdate: ${config.repoAutoUpdate}")
            add(" usingBackupRepo: ${HanniRepoManager.isUsingBackup}")
            if (hasDefaultSettings) {
                add((" repo location: default"))
            } else {
                add(" non-default repo location: '${HanniRepoManager.getGitHubRepoPath()}'")
            }

            if (unsuccessfulConstants.isNotEmpty()) {
                add(" unsuccessful constants:")
                for (constant in unsuccessfulConstants) {
                    add("  - $constant")
                }
            }

            val neuRepoConfig = HanniMod.feature.dev.neuRepo
            add(" neuRepoAutoUpdate: ${neuRepoConfig.repoAutoUpdate}")

            if (!neuRepoConfig.location.hasDefaultSettings()) {
                add(" neu repo location: '${EnoughUpdatesRepoManager.getGitHubRepoPath()}'")
            } else {
                add(" neu repo location: default")
            }

            add(" loaded neu items: ${NeuItems.allNeuRepoItems().size}")
        }

        val isRelevant = HanniRepoManager.isUsingBackup || unsuccessfulConstants.isNotEmpty() || !hasDefaultSettings
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
        event.title("Network Information")
        val tps = TpsCounter.tps ?: 0.0
        val pingEnabled = HanniMod.feature.dev.pingApi

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
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shdebug") {
            description = "Copies Hanni debug data in the clipboard."
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
