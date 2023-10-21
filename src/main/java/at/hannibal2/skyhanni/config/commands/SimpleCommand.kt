package at.hannibal2.skyhanni.config.commands

import at.hannibal2.skyhanni.test.command.ErrorManager
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.util.BlockPos

class SimpleCommand : CommandBase {
    private val commandName: String
    private val runnable: ProcessCommandRunnable
    private var tabRunnable: TabCompleteRunnable? = null

    constructor(commandName: String, runnable: ProcessCommandRunnable) {
        this.commandName = commandName
        this.runnable = runnable
    }

    constructor(commandName: String, runnable: ProcessCommandRunnable, tabRunnable: TabCompleteRunnable?) {
        this.commandName = commandName
        this.runnable = runnable
        this.tabRunnable = tabRunnable
    }

    abstract class ProcessCommandRunnable {
        abstract fun processCommand(sender: ICommandSender?, args: Array<String>?)
    }

    interface TabCompleteRunnable {
        fun tabComplete(sender: ICommandSender?, args: Array<String>?, pos: BlockPos?): List<String>
    }

    override fun canCommandSenderUseCommand(sender: ICommandSender) = true

    override fun getCommandName() = commandName

    override fun getCommandUsage(sender: ICommandSender) = "/$commandName"

    override fun processCommand(sender: ICommandSender, args: Array<String>) {
        try {
            runnable.processCommand(sender, args)
        } catch (e: Throwable) {
            ErrorManager.logError(e, "Error while running command /$commandName")
        }
    }

    override fun addTabCompletionOptions(sender: ICommandSender, args: Array<String>, pos: BlockPos) =
        if (tabRunnable != null) tabRunnable!!.tabComplete(sender, args, pos) else null
}