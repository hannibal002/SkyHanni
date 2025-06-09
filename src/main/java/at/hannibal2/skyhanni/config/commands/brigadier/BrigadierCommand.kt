package at.hannibal2.skyhanni.config.commands.brigadier

import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.tree.CommandNode
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.util.BlockPos

class BrigadierCommand(
    root: BaseBrigadierBuilder,
    private val dispatcher: CommandDispatcher<Any?>
) : CommandBase() {
    private val aliases: List<String> = root.aliases
    private val node: CommandNode<Any?>

    init {
        val builder = root.builder
        node = builder.build()
        root.node = node
        dispatcher.register(builder as LiteralArgumentBuilder<Any?>)
    }

    override fun getCommandName(): String = node.name
    override fun getCommandUsage(sender: ICommandSender): String = "/${node.name}"

    override fun getCommandAliases() = aliases
    override fun canCommandSenderUseCommand(sender: ICommandSender) = true

    private fun getSmartUsage(): List<String> {
        val map = dispatcher.getSmartUsage(node, null)
        if (map.isEmpty()) return emptyList()
        return map.entries.map { "§e/${node.name} ${it.value}" }
    }

    override fun processCommand(sender: ICommandSender, args: Array<String>) {
        val input = if (args.isEmpty()) node.name else "${node.name} ${args.joinToString(" ")}"
        try {
            dispatcher.execute(input, sender)
        } catch (e: CommandSyntaxException) {
            val message = e.message ?: "Error when parsing command."
            val shouldShowUsage = message.startsWith("Unknown command") || message.startsWith("Incorrect argument")
            val usage = getSmartUsage()
            if (!shouldShowUsage || usage.isEmpty()) {
                ChatUtils.userError(message)
            } else {
                ChatUtils.hoverableChat(
                    "§cWrong command usage for /${node.name} (hover to see usage)",
                    usage,
                    prefixColor = "§c",
                )
            }
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Failed to execute command")
        }
    }

    override fun addTabCompletionOptions(
        sender: ICommandSender,
        args: Array<String>,
        pos: BlockPos
    ): List<String> {
        val input = if (args.isEmpty()) node.name else "${node.name} ${args.joinToString(" ")}"
        return runCatching {
            dispatcher.getCompletionSuggestions(dispatcher.parse(input, sender)).get().list.map { it.text }
        }.getOrDefault(emptyList())
    }

}
