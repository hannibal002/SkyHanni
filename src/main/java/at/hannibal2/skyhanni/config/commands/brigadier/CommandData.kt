package at.hannibal2.skyhanni.config.commands.brigadier

import at.hannibal2.skyhanni.config.commands.CommandCategory

interface CommandData {
    val name: String
    var aliases: List<String>
    var category: CommandCategory
    val descriptor: String

    fun getAllNames(): List<String> {
        val allNames = mutableListOf(name)
        allNames.addAll(aliases)
        return allNames
    }
}
