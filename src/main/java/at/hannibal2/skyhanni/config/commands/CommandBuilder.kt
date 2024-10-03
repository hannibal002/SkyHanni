package at.hannibal2.skyhanni.config.commands

class CommandBuilder(val name: String) {
    var description: String = ""
    var category: CommandCategory = CommandCategory.MAIN
    var aliases: List<String> = emptyList()
    var autoComplete: ((Array<String>) -> List<String>) = { listOf() }
    var callback: (Array<String>) -> Unit = {}

    fun toSimpleCommand() = SimpleCommand(name.lowercase(), aliases, callback, autoComplete)
}

