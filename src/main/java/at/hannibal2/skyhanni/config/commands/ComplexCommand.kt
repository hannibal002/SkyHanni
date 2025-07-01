package at.hannibal2.skyhanni.config.commands

import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CommandArgument
import at.hannibal2.skyhanni.utils.CommandArgument.Companion.findSpecifierAndGetResult
import at.hannibal2.skyhanni.utils.CommandContextAwareObject
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.util.BlockPos

data class ComplexCommand<O : CommandContextAwareObject>(
    val name: String,
    val specifiers: Collection<CommandArgument<O>>,
    val context: (ComplexCommand<O>) -> O,
    val aliases: List<String>,
) : CommandBase() {

    override fun canCommandSenderUseCommand(sender: ICommandSender) = true
    override fun getCommandName() = name
    override fun getCommandAliases() = aliases
    override fun getCommandUsage(sender: ICommandSender) = "/$name"

    override fun processCommand(sender: ICommandSender, args: Array<String>) {
        try {
            handleCommand(args)
        } catch (e: Throwable) {
            ErrorManager.logErrorWithData(e, "Error while running command /$name")
        }
    }

    fun constructHelp(description: String): String = buildString {
        appendLine(description)
        appendLine()
        appendLine("Arguments:")
        specifiers
            .sortedBy {
                when (it.defaultPosition) {
                    -1 -> Int.MAX_VALUE
                    -2 -> Int.MAX_VALUE - 1
                    else -> it.defaultPosition
                }
            }
            .forEach {
                if (it.prefix.isNotEmpty()) {
                    if (it.defaultPosition != -1) {
                        appendLine("[${it.prefix}] ${it.documentation}")
                    } else {
                        appendLine("${it.prefix} ${it.documentation}")
                    }
                } else {
                    appendLine(it.documentation)
                }
            }
        deleteAt(lastIndex) // Removes the last /n
    }

    private fun handleCommand(args: Array<String>) {
        val context = context(this)
        var index = 0
        var amountNoPrefixArguments = 0

        while (args.size > index) {
            val step = specifiers.findSpecifierAndGetResult(args, index, context, amountNoPrefixArguments) { amountNoPrefixArguments++ }
            context.errorMessage?.let {
                ChatUtils.userError(it)
                return
            }
            index += step
        }
        context.post()
        context.errorMessage?.let {
            ChatUtils.userError(it)
            return
        }
    }

    private fun tabParse(args: Array<String>, partial: String?): List<String> {
        val context = context(this)

        var index = 0
        var amountNoPrefixArguments = 0

        while (args.size > index) {
            val loopStartAmountNoPrefix = amountNoPrefixArguments
            val step = specifiers.findSpecifierAndGetResult(args, index, context, amountNoPrefixArguments) { amountNoPrefixArguments++ }
            if (context.errorMessage != null) {
                if (loopStartAmountNoPrefix != amountNoPrefixArguments) {
                    amountNoPrefixArguments = loopStartAmountNoPrefix
                }
                break
            }
            index += step
        }

        val result = mutableListOf<String>()

        val validSpecifier = specifiers.filter { it.validity(context) }

        val rest = (args.slice(index..<args.size).joinToString(" ") + (partial?.let { " $it" }.orEmpty())).trimStart()

        if (rest.isEmpty()) {
            result.addAll(validSpecifier.mapNotNull { it.prefix.takeIf { i -> i.isNotEmpty() } })
            result.addAll(
                validSpecifier.filter { it.defaultPosition == amountNoPrefixArguments }.map { it.tabComplete("", context) }
                    .flatten(),
            )
        } else {
            result.addAll(
                validSpecifier.filter { it.prefix.startsWith(rest) }.mapNotNull { it.prefix.takeIf { i -> i.isNotEmpty() } },
            )
            result.addAll(
                validSpecifier.filter { it.defaultPosition == amountNoPrefixArguments }.map { it.tabComplete(rest, context) }
                    .flatten(),
            )
        }

        return result
    }

    override fun addTabCompletionOptions(sender: ICommandSender, args: Array<String>, pos: BlockPos): List<String>? {
        val rawArgs = args.toList()
        val isPartial = rawArgs.last().isNotEmpty()
        val newArgs = if (isPartial) rawArgs.dropLast(1) else rawArgs

        val partial = if (isPartial) rawArgs.last() else null

        return tabParse(newArgs.toTypedArray(), partial)
    }
}
