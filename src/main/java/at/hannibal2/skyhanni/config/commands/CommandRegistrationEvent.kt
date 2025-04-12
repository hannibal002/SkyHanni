package at.hannibal2.skyhanni.config.commands

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.config.commands.CommandsRegistry.addToRegister
import at.hannibal2.skyhanni.config.commands.CommandsRegistry.hasUniqueName
import at.hannibal2.skyhanni.utils.CommandArgument
import at.hannibal2.skyhanni.utils.CommandContextAwareObject
//#if MC < 1.21
import net.minecraftforge.client.ClientCommandHandler
//#else
//$$ import com.mojang.brigadier.arguments.StringArgumentType
//$$ import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
//$$ import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
//$$ import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
//#endif

class CommandRegistrationEvent(private val builders: MutableList<CommandBuilderBase>) : SkyHanniEvent() {

    val commands: List<CommandBuilderBase> get() = builders

    fun register(name: String, block: CommandBuilder.() -> Unit) {
        val command = CommandBuilder(name).apply(block)
        command.hasUniqueName()
        if (command.description.isEmpty() && command.category !in CommandCategory.developmentCategories) {
            error("The command '$name' has no description!")
        }
        command.addToRegister()
    }

    fun <O : CommandContextAwareObject> registerComplex(
        name: String, block: ComplexCommandBuilder<O, CommandArgument<O>>.() -> Unit,
    ) {
        val command = ComplexCommandBuilder<O, CommandArgument<O>>(name).apply(block)
        command.hasUniqueName()
        command.addToRegister()
    }
}
