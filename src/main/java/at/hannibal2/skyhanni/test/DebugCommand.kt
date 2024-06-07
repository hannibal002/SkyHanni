package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.repo.RepoManager
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.equalsIgnoreColor

object DebugCommand {

    fun command(args: Array<String>) {
        if (args.size == 2 && args[0] == "profileName") {
            HypixelData.profileName = args[1].lowercase()
            ChatUtils.chat("§eManually set profileName to '${HypixelData.profileName}'")
            return
        }
        val list = mutableListOf<String>()
        list.add("```")
        list.add("= Debug Information for SkyHanni ${SkyHanniMod.version} =")
        list.add("")

        val search = args.joinToString(" ")
        list.add(
            if (search.isNotEmpty()) {
                if (search.equalsIgnoreColor("all")) {
                    "search for everything:"
                } else "search '$search':"
            } else "no search specified, only showing interesting stuff:"
        )

        val event = DebugDataCollectEvent(list, search)

        // calling default debug stuff
        player(event)
        repoAutoUpdate(event)
        repoLocation(event)
        globalRender(event)
        skyblockStatus(event)
        profileName(event)
        profileType(event)

        event.postAndCatch()

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
        if (!LorenzUtils.inSkyBlock) {
            event.addIrrelevant("Not on SkyBlock")
            return
        }

        if (ProfileStorageData.playerSpecific == null) {
            event.addData("playerSpecific is null!")
            return
        }

        val classic = !LorenzUtils.noTradeMode
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
        if (!LorenzUtils.inSkyBlock) {
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
        if (!LorenzUtils.onHypixel) {
            event.addData("not on Hypixel")
            return
        }
        if (!LorenzUtils.inSkyBlock) {
            event.addData("not on SkyBlock, but on Hypixel")
            return
        }
        if (LorenzUtils.skyBlockIsland == IslandType.UNKNOWN) {
            event.addData("Unknown SkyBlock island!")
            return
        }
        event.addIrrelevant {
            add("on Hypixel SkyBlock")
            add("skyBlockIsland: ${LorenzUtils.skyBlockIsland}")
            add("skyBlockArea: '${LorenzUtils.skyBlockArea}'")
        }
    }

    private fun globalRender(event: DebugDataCollectEvent) {
        event.title("Global Render")
        if (SkyHanniDebugsAndTests.globalRender) {
            event.addIrrelevant("normal enabled")
        } else {
            event.addData {
                add("Global renderer is disabled!")
                add("No renderable elements from SkyHanni will show up anywhere!")
            }
        }
    }

    private fun repoAutoUpdate(event: DebugDataCollectEvent) {
        event.title("Repo Auto Update")
        if (SkyHanniMod.feature.dev.repo.repoAutoUpdate) {
            event.addIrrelevant("normal enabled")
        } else {
            event.addData("The repo does not auto update because auto update is disabled!")
        }
    }

    private fun repoLocation(event: DebugDataCollectEvent) {
        event.title("Repo Location")
        event.addIrrelevant("repo location: '${RepoManager.getRepoLocation()}'")
    }

    private fun player(event: DebugDataCollectEvent) {
        event.title("Player")
        event.addIrrelevant {
            add("name: '${LorenzUtils.getPlayerName()}'")
            add("uuid: '${LorenzUtils.getPlayerUuid()}'")
        }
    }
}
