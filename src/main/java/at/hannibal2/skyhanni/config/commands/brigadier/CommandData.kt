package at.hannibal2.skyhanni.config.commands.brigadier

import at.hannibal2.skyhanni.config.commands.CommandCategory
import com.mojang.brigadier.CommandDispatcher
//#if MC < 1.21
import net.minecraft.command.ICommand
//#endif

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

    //#if MC < 1.21
    fun toCommand(dispatcher: CommandDispatcher<Any?>): ICommand
    //#endif
}
