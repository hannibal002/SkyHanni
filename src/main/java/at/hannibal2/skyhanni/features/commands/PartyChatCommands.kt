package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.features.misc.PartyCommandsConfig
import at.hannibal2.skyhanni.data.FriendApi
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.data.hypixel.chat.event.PartyChatEvent
import at.hannibal2.skyhanni.events.chat.TabCompletionEvent
import at.hannibal2.skyhanni.features.misc.CurrentPing
import at.hannibal2.skyhanni.features.misc.TpsCounter
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PartyChatCommands {
    private val config get() = SkyHanniMod.feature.misc.partyCommands
    private val storage get() = SkyHanniMod.feature.storage
    private val devConfig get() = SkyHanniMod.feature.dev

    data class PartyChatCommand(
        val names: List<String>,
        val isEnabled: () -> Boolean,
        val requiresPartyLead: Boolean = true,
        val triggerableBySelf: Boolean = true,
        val executable: (PartyChatEvent) -> Unit,
    )

    private var lastWarp = SimpleTimeMark.farPast()
    private var lastAllInvite = SimpleTimeMark.farPast()

    private val allPartyCommands = listOf(
        PartyChatCommand(
            listOf("pt", "ptme", "transfer"),
            { config.transferCommand },
            triggerableBySelf = false,
            executable = {
                HypixelCommands.partyTransfer(it.cleanedAuthor)
            },
        ),
        PartyChatCommand(
            listOf("pw", "warp", "warpus"),
            { config.warpCommand && lastWarp.passedSince() > 5.seconds },
            executable = {
                lastWarp = SimpleTimeMark.now()
                HypixelCommands.partyWarp()
            },
        ),
        PartyChatCommand(
            listOf("allinv", "allinvite"),
            { config.allInviteCommand && lastAllInvite.passedSince() > 2.seconds },
            executable = {
                lastAllInvite = SimpleTimeMark.now()
                HypixelCommands.partyAllInvite()
            },
        ),
        PartyChatCommand(
            listOf("ping"),
            { config.pingCommand },
            requiresPartyLead = false,
            executable = {

                if (!devConfig.hypixelPingApi) {

                    ChatUtils.clickableChat(
                        "Hypixel Ping Api is disabled, ping command won't work!",
                        prefixColor = "§c",
                        onClick = {
                            devConfig::hypixelPingApi.jumpToEditor()
                        },
                        hover = "§eClick to find setting in the config!",
                    )
                    return@PartyChatCommand
                }
                HypixelCommands.partyChat("Current Ping: ${CurrentPing.averagePing.inWholeMilliseconds.addSeparators()}ms", prefix = true)

            },
        ),
        PartyChatCommand(
            listOf("tps"),
            { config.tpsCommand },
            requiresPartyLead = false,
            executable = {
                if (TpsCounter.tps != null) {
                    HypixelCommands.partyChat("Current TPS: ${TpsCounter.tps}", prefix = true)
                } else {
                    ChatUtils.chat("TPS Command Sent too early to calculate TPS")
                }
            },
        ),
    )

    private val indexedPartyChatCommands = buildMap {
        for (command in allPartyCommands) {
            for (name in command.names) {
                put(name.lowercase(), command)
            }
        }
    }

    private fun isTrustedUser(name: String): Boolean {
        if (name == PlayerUtils.getName()) return true
        val friend = FriendApi.getAllFriends().find { it.name == name }
        return when (config.defaultRequiredTrustLevel) {
            PartyCommandsConfig.TrustedUser.FRIENDS -> friend != null
            PartyCommandsConfig.TrustedUser.BEST_FRIENDS -> friend?.bestFriend == true
            PartyCommandsConfig.TrustedUser.ANYONE -> true
            PartyCommandsConfig.TrustedUser.NO_ONE -> false
        }
    }

    private val commandPrefixes = ".!?".toSet()

    private fun isBlockedUser(name: String): Boolean {
        return storage.blacklistedUsers.any { it.equals(name, ignoreCase = true) }
    }

    @HandleEvent
    fun onPartyCommand(event: PartyChatEvent) {
        if (event.message.firstOrNull() !in commandPrefixes) return
        val commandLabel = event.message.substring(1).substringBefore(' ')
        val command = indexedPartyChatCommands[commandLabel.lowercase()] ?: return
        val name = event.cleanedAuthor
        if (name == PlayerUtils.getName() && (!command.triggerableBySelf || !config.selfTriggerCommands)) return
        if (!command.isEnabled()) return
        if (command.requiresPartyLead && PartyApi.partyLeader != PlayerUtils.getName()) return
        if (isBlockedUser(name)) {
            if (config.showIgnoredReminder) ChatUtils.clickableChat(
                "§cIgnoring chat command from ${event.author}. " +
                    "Stop ignoring them using /shignore remove <player> or click here!",
                onClick = { blacklistModify(event.author) },
                "§eClick to ignore ${event.author}!",
            )
            return
        }
        if (!isTrustedUser(name)) {
            if (config.showIgnoredReminder) {
                ChatUtils.chat(
                    "§cIgnoring chat command from $name. " +
                        "Change your party chat command settings or /friend (best) them.",
                )
            }
            return
        }
        command.executable(event)
    }

    @HandleEvent
    fun onTabComplete(event: TabCompletionEvent) {
        if (PartyApi.partyLeader == null) return
        val prefix = event.fullText.firstOrNull() ?: return
        if (prefix !in commandPrefixes) return

        val commandText = event.fullText.substring(1)
        indexedPartyChatCommands.keys
            .filter { it.startsWith(commandText) }
            .map { "$prefix$it" }
            .forEach(event::addSuggestion)
    }

    @HandleEvent
    fun onCommandRegister(event: CommandRegistrationEvent) {
        event.registerBrigadier("shignore") {
            description = "Add/Remove a user from your blacklist"
            category = CommandCategory.USERS_ACTIVE

            literal("add") {
                arg("name", BrigadierArguments.string()) { nameArg ->
                    callback {
                        val name = getArg(nameArg)
                        if (isBlockedUser(name)) {
                            ChatUtils.userError("$name is already ignored!")
                        } else blacklistModify(name)
                    }
                }
            }

            literal("remove") {
                arg("name", BrigadierArguments.string()) { nameArg ->
                    callback {
                        val name = getArg(nameArg)
                        if (!isBlockedUser(name)) {
                            ChatUtils.userError("$name isn't ignored!")
                        } else blacklistModify(name)
                    }
                }
            }
            literal("list") {
                argCallback("name", BrigadierArguments.string()) { name ->
                    blacklistView(name)
                }
                callback {
                    blacklistView()
                }
            }
            literalCallback("clear") {
                ChatUtils.clickableChat(
                    "Are you sure you want to do this? Click here to confirm.",
                    onClick = {
                        storage.blacklistedUsers.clear()
                        ChatUtils.chat("Cleared your ignored players list!")
                    },
                    "§eClick to confirm.",
                    oneTimeClick = true,
                )
            }
            argCallback("name", BrigadierArguments.string()) { name ->
                blacklistModify(name)
            }
        }
    }

    private fun blacklistModify(player: String) {
        if (player !in storage.blacklistedUsers) {
            ChatUtils.chat("§cNow ignoring §b$player§e!")
            storage.blacklistedUsers.add(player)
            return
        }
        ChatUtils.chat("§aStopped ignoring §b$player§e!")
        storage.blacklistedUsers.remove(player)
        return
    }

    private fun blacklistView() {
        val blacklist = storage.blacklistedUsers
        if (blacklist.size <= 0) {
            ChatUtils.chat("Your ignored players list is empty!")
            return
        }
        var message = "Ignored player list:"
        if (blacklist.size > 15) {
            message += "\n§e"
            blacklist.forEachIndexed { i, blacklistedMessage ->
                message += blacklistedMessage
                if (i < blacklist.size - 1) {
                    message += ", "
                }
            }
        } else {
            blacklist.forEach { message += "\n§e$it" }
        }
        ChatUtils.chat(message)
    }

    private fun blacklistView(player: String) {
        if (isBlockedUser(player)) {
            ChatUtils.chat("$player §ais §eignored.")
        } else {
            ChatUtils.chat("$player §cisn't §eignored.")
        }
    }
}
