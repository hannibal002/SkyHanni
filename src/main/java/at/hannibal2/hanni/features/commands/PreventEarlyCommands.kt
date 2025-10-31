package at.hannibal2.hanni.features.commands

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.MessageSendToServerEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.events.minecraft.WorldChangeEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.ChatUtils.senderIsHanni
import at.hannibal2.hanni.utils.DelayedRun
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.StringUtils
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@HanniModule
object PreventEarlyCommands {
    private val config get() = HanniMod.feature.misc.commands

    private var commandExecuted: SimpleTimeMark = SimpleTimeMark.farPast()
    private var worldChanged: SimpleTimeMark = SimpleTimeMark.farPast()
    private var lastCommand: String? = null

    /**
     * REGEX-TEST: §cYou may only use this command after 4s on the server!
     */
    private val cooldownPattern by RepoPattern.pattern(
        "commands.cooldown",
        "§cYou may only use this command after (?<cooldown>\\d+)s on the server!",
    )

    @HandleEvent
    fun onMessageSendToServer(event: MessageSendToServerEvent) {
        if (!config.preventEarlyExecution) return
        if (!SkyBlockUtils.onHypixel) return
        if (!event.isCommand) return
        if (event.senderIsHanni()) return
        val command = event.message.removePrefix("/").lowercase()
        if (command == "locraw") return // Ignore locraw commands
        lastCommand = command

        commandExecuted = SimpleTimeMark.now()
    }

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        worldChanged = SimpleTimeMark.now()
        lastCommand = null
    }

    @HandleEvent
    fun onChat(event: HanniChatEvent) {
        if (!SkyBlockUtils.onHypixel) return
        if (!config.preventEarlyExecution) return
        val lastCommand = lastCommand ?: return
        cooldownPattern.matchMatcher(event.message) {
            val cooldown = group("cooldown")
            val runIn: Duration = (cooldown?.toInt()?.seconds ?: 5.seconds) - worldChanged.absoluteDifference(SimpleTimeMark.now())
            DelayedRun.runDelayed(runIn) {
                ChatUtils.sendMessageToServer("/$lastCommand")
            }
            event.blockedReason = "prevent_early_command"
            val seconds = ceil(runIn.toDouble(DurationUnit.SECONDS)).toInt()
            val formattedTime = "$seconds ${StringUtils.pluralize(seconds, "second")}"

            ChatUtils.chat("§cCannot execute §e/$lastCommand §cyet. §aRunning it in $formattedTime.")
        }
    }
}
