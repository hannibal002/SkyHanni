package at.hannibal2.skyhanni.config.commands

import net.minecraft.command.ICommandSender
import net.minecraft.util.BlockPos

// TODO: Gravy's dm
class CommandBuilder(val name: String) {
    var description: String = ""
    var category: CommandCategory = CommandCategory.MAIN
    var aliases: List<String> = emptyList()
    var autoComplete: ((Array<String>) -> List<String>) = { listOf() }
    var callback: (Array<String>) -> Unit = {}

    fun toSimpleCommand() = SimpleCommand(
        name.lowercase(),
        aliases,
        createCommand(callback),
        object : SimpleCommand.TabCompleteRunnable {
            override fun tabComplete(
                sender: ICommandSender?,
                args: Array<String>?,
                pos: BlockPos?,
            ): List<String> {
                return autoComplete(args ?: emptyArray())
            }
        },
    )

    private fun createCommand(function: (Array<String>) -> Unit) = object : SimpleCommand.ProcessCommandRunnable() {
        override fun processCommand(sender: ICommandSender?, args: Array<String>?) {
            if (args != null) function(args.asList().toTypedArray())
        }
    }
}

