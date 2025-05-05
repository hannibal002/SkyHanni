package at.hannibal2.skyhanni.config.commands

import at.hannibal2.skyhanni.config.commands.brigadier.CommandData
import at.hannibal2.skyhanni.utils.CommandArgument
import at.hannibal2.skyhanni.utils.CommandContextAwareObject
import com.mojang.brigadier.CommandDispatcher
import net.minecraft.command.ICommand

class CommandBuilder(name: String) : CommandBuilderBase(name) {
    private var autoComplete: ((Array<String>) -> List<String>) = { listOf() }
    private var callback: (Array<String>) -> Unit = {}

    fun callback(callback: (Array<String>) -> Unit) {
        this.callback = callback
    }

    fun autoComplete(autoComplete: (Array<String>) -> List<String>) {
        this.autoComplete = autoComplete
    }

    override fun toCommand(dispatcher: CommandDispatcher<Any?>) = SimpleCommand(name.lowercase(), aliases, callback, autoComplete)
}

abstract class CommandBuilderBase(override val name: String) : CommandData {
    var description: String = ""
    override var category: CommandCategory = CommandCategory.MAIN
    override var aliases: List<String> = emptyList()

    override val descriptor: String get() = description
}

class ComplexCommandBuilder<O : CommandContextAwareObject, A : CommandArgument<O>>(name: String) : CommandBuilderBase(name) {
    lateinit var specifiers: Collection<A>
    lateinit var context: (ComplexCommand<O>) -> O

    private var realDescription: String = ""

    override fun toCommand(dispatcher: CommandDispatcher<Any?>): ICommand {
        return ComplexCommand(name.lowercase(), specifiers, context, aliases).also {
            realDescription = it.constructHelp(description)
        }
    }

    override val descriptor get() = realDescription
}

