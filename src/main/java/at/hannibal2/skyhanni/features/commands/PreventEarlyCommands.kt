package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ChatUtils.senderIsSkyhanni
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.withColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.ChatFormatting
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@SkyHanniModule
object PreventEarlyCommands {
    private val config get() = SkyHanniMod.feature.misc.commands

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
        if (event.senderIsSkyhanni()) return
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
    fun onChat(event: SkyHanniChatEvent.Allow) {
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

            ChatUtils.chat(
                componentBuilder {
                    withColor(ChatFormatting.RED)
                    append("Cannot execute ")
                    append("/$lastCommand ") {
                        withColor(ChatFormatting.YELLOW)
                    }
                    append("yet. ")
                    append("Running it in $formattedTime.") {
                        withColor(ChatFormatting.GREEN)

                    }
                }
            )
        }
    }
}
