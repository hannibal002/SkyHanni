package at.hannibal2.skyhanni.config.commands

import at.hannibal2.skyhanni.config.commands.brigadier.CommandData

class CommandBuilder(override val name: String) : CommandData {
    var description: String = ""
    override var category: CommandCategory = CommandCategory.MAIN
    override var aliases: List<String> = emptyList()
    override val descriptor: String get() = description

    private var callback: (Array<String>) -> Unit = {}

    fun callback(callback: (Array<String>) -> Unit) {
        this.callback = callback
    }

    fun getCallback(): (Array<String>) -> Unit = callback
}
