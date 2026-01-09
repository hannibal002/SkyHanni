package at.hannibal2.skyhanni.config.commands

import at.hannibal2.skyhanni.config.commands.brigadier.CommandData
import at.hannibal2.skyhanni.utils.CommandArgument
import at.hannibal2.skyhanni.utils.CommandContextAwareObject

class CommandBuilder(name: String) : CommandBuilderBase(name) {
    private var autoComplete: ((Array<String>) -> List<String>) = { listOf() }
    private var callback: (Array<String>) -> Unit = {}

    fun callback(callback: (Array<String>) -> Unit) {
        this.callback = callback
    }

    // Used for command registration in 1.21.5
    @Suppress("unused")
    fun getCallback(): (Array<String>) -> Unit = callback

    fun autoComplete(autoComplete: (Array<String>) -> List<String>) {
        this.autoComplete = autoComplete
    }
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

