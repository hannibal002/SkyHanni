package at.hannibal2.skyhanni.config.commands

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.config.commands.CommandsRegistry.addToRegister
import at.hannibal2.skyhanni.config.commands.CommandsRegistry.hasUniqueName
import at.hannibal2.skyhanni.config.commands.brigadier.BaseBrigadierBuilder
import at.hannibal2.skyhanni.config.commands.brigadier.CommandData
import at.hannibal2.skyhanni.utils.CommandArgument
import at.hannibal2.skyhanni.utils.CommandContextAwareObject
import com.mojang.brigadier.CommandDispatcher

class CommandRegistrationEvent(
    val dispatcher: CommandDispatcher<Any?>,
) : SkyHanniEvent() {
    private val builders = mutableListOf<CommandData>()

    val commands: List<CommandData> get() = builders

    fun registerBrigadier(name: String, builder: BaseBrigadierBuilder.() -> Unit) {
        val command = BaseBrigadierBuilder(name).apply(builder)
        command.hasUniqueName(builders)
        command.checkDescriptionAndCategory()
        command.addToRegister(dispatcher, builders)
    }

    // TODO: Use Brigadier as backend and eventually deprecate it
    fun register(name: String, block: CommandBuilder.() -> Unit) {
        val command = CommandBuilder(name).apply(block)
        command.hasUniqueName(builders)
        command.checkDescriptionAndCategory()
        command.addToRegister(dispatcher, builders)
    }

    private fun CommandData.checkDescriptionAndCategory() {
        require(descriptor.isNotEmpty() || category in CommandCategory.developmentCategories) {
            "The command '$name' has no required description"
        }
    }

    fun <O : CommandContextAwareObject> registerComplex(
        name: String,
        block: ComplexCommandBuilder<O, CommandArgument<O>>.() -> Unit,
    ) {
        val command = ComplexCommandBuilder<O, CommandArgument<O>>(name).apply(block)
        command.hasUniqueName(builders)
        command.addToRegister(dispatcher, builders)
    }
}
