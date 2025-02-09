package at.hannibal2.skyhanni.config.commands

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.utils.PreInitFinishedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraftforge.client.ClientCommandHandler

@SkyHanniModule
object CommandsRegistry {
    private val builders = mutableListOf<CommandBuilderBase>()

    @HandleEvent
    fun onPreInitFinished(event: PreInitFinishedEvent) {
        CommandRegistrationEvent(builders).post()
    }

    private fun String.isUnique() {
        if (builders.any { it.name == this || it.aliases.contains(this) }) {
            error("The command '$this is already registered!'")
        }
    }

    fun <T : CommandBuilderBase> T.hasUniqueName() {
        name.isUnique()
        aliases.forEach { it.isUnique() }
    }

    fun <T : CommandBuilderBase> T.addToRegister() {
        ClientCommandHandler.instance.registerCommand(this.toCommand())
        builders.add(this)
    }
}
