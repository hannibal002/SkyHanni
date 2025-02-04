package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ChatHoverEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.event.CommandEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toDuration
import kotlin.time.toJavaDuration

@SkyHanniModule
object PreventEarlyCommands {
    private var commandExecuted: SimpleTimeMark = SimpleTimeMark.farPast()
    private var worldChanged: SimpleTimeMark = SimpleTimeMark.farPast()
    private var command: String = ""

    /**
     * REGEX-TEST: §cYou may only use this command after 4s on the server!
     */
    private val cooldownPattern by RepoPattern.pattern(
        "commands.cooldown",
        "§cYou may only use this command after (?<cooldown>\\d+)s on the server!"
    )

    @SubscribeEvent
    fun onCommand(event: CommandEvent) {
        command = event.command.commandName
        ChatUtils.debug("Setting command to $command")
        commandExecuted = SimpleTimeMark.now()
    }

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        worldChanged = SimpleTimeMark.now()
    }

    @HandleEvent
    fun onRecieveChatMessage(event: SkyHanniChatEvent) {
        if(cooldownPattern.matches(event.message)) {
            val cooldown = cooldownPattern.matchMatcher(event.message) {
                group("cooldown")
            }
            val runIn: Duration = worldChanged.absoluteDifference(SimpleTimeMark.now()) + (cooldown?.toInt()?.seconds ?: 5.seconds)
            DelayedRun.runDelayed(runIn) {
                ChatUtils.sendMessageToServer("/$command")
            }
            event.blockedReason = "prevent_early_command"
            ChatUtils.chat("Cannot execute /$command yet. Running in $runIn seconds.")
        }
    }
}
