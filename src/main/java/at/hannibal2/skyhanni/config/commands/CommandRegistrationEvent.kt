package at.hannibal2.skyhanni.config.commands

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.config.commands.Commands.commands
import net.minecraftforge.client.ClientCommandHandler

object CommandRegistrationEvent : SkyHanniEvent() {
    fun register(name: String, block: CommandBuilder.() -> Unit) {
        val info = CommandBuilder(name).apply(block)
        if (commands.any { it.name == name }) {
            error("The command '$name is already registered!'")
        }
        ClientCommandHandler.instance.registerCommand(info.toSimpleCommand())
        commands.add(info)
    }
}
