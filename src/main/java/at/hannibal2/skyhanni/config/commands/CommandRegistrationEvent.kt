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
    private val builders: MutableList<CommandData>,
    val dispatcher: CommandDispatcher<Any?>,
) : SkyHanniEvent() {

    val commands: List<CommandData> get() = builders

    fun registerBrigadier(name: String, builder: BaseBrigadierBuilder.() -> Unit) {
        val command = BaseBrigadierBuilder(name).apply(builder)
        command.hasUniqueName()
        command.checkDescriptionAndCategory()
        command.addToRegister(dispatcher)
    }

    // TODO: Use Brigadier as backend and eventually deprecate it
    fun register(name: String, block: CommandBuilder.() -> Unit) {
        val command = CommandBuilder(name).apply(block)
        command.hasUniqueName()
        command.checkDescriptionAndCategory()
        command.addToRegister(dispatcher)
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
        command.hasUniqueName()
        command.addToRegister(dispatcher)
    }
}
