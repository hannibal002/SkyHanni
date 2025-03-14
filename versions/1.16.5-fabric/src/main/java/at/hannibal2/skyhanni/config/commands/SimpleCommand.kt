package at.hannibal2.skyhanni.config.commands

import at.hannibal2.skyhanni.test.command.ErrorManager

class SimpleCommand(
    private val name: String,
    private val aliases: List<String>,
    private val callback: (Array<String>) -> Unit,
    // TODO modern version reimplement this
    private val tabCallback: ((Array<String>) -> List<String>) = { emptyList() },
) {

    fun getCommandName() = name
    fun getCommandAliases() = aliases

    fun processCommand(args: Array<String>) {
        try {
            callback(args)
        } catch (e: Throwable) {
            ErrorManager.logErrorWithData(e, "Error while running command /$name")
        }
    }
}
