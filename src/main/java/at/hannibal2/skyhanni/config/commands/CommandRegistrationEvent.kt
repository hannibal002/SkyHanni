package at.hannibal2.skyhanni.config.commands

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.CommandArgument
import at.hannibal2.skyhanni.utils.CommandContextAwareObject
import net.minecraftforge.client.ClientCommandHandler

class CommandRegistrationEvent(private val builders: MutableList<CommandBuilderBase>) : SkyHanniEvent() {

    val commands: List<CommandBuilderBase> get() = builders

    private fun String.isUnique() {
        if (builders.any { it.name == this || it.aliases.contains(this) }) {
            error("The command '$this is already registered!'")
        }
    }

    private fun <T : CommandBuilderBase> T.hasUniqueName() {
        name.isUnique()
        aliases.forEach { it.isUnique() }
    }

    private fun <T : CommandBuilderBase> T.add() {
        ClientCommandHandler.instance.registerCommand(this.toCommand())
        builders.add(this)
    }

    fun register(name: String, block: CommandBuilder.() -> Unit) {
        val command = CommandBuilder(name).apply(block)
        command.hasUniqueName()
        if (command.description.isEmpty() && command.category !in CommandCategory.developmentCategories) {
            error("The command '$name' has no description!")
        }
        command.add()
    }

    fun <O : CommandContextAwareObject> registerComplex(
        name: String, block: ComplexCommandBuilder<O, CommandArgument<O>>.() -> Unit,
    ) {
        val command = ComplexCommandBuilder<O, CommandArgument<O>>(name).apply(block)
        command.hasUniqueName()
        command.add()
    }
}
