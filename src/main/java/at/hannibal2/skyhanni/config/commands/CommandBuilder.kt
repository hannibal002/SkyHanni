package at.hannibal2.skyhanni.config.commands

import at.hannibal2.skyhanni.config.commands.brigadier.CommandData
import at.hannibal2.skyhanni.utils.CommandArgument
import at.hannibal2.skyhanni.utils.CommandContextAwareObject

class CommandBuilder(name: String) : CommandBuilderBase(name) {
    private var callback: (Array<String>) -> Unit = {}

    fun callback(callback: (Array<String>) -> Unit) {
        this.callback = callback
    }

    fun getCallback(): (Array<String>) -> Unit = callback
}

sealed class CommandBuilderBase(override val name: String) : CommandData {
    var description: String = ""
    override var category: CommandCategory = CommandCategory.MAIN
    override var aliases: List<String> = emptyList()

    override val descriptor: String get() = description
}

class ComplexCommandBuilder<O : CommandContextAwareObject, A : CommandArgument<O>>(name: String) : CommandBuilderBase(name) {
    lateinit var specifiers: Collection<A>

    private var realDescription: String = ""


    override val descriptor get() = realDescription
}

