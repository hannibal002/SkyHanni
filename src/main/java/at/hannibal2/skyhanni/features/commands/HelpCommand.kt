package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BaseBrigadierBuilder
import at.hannibal2.skyhanni.config.commands.brigadier.CommandData
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.StringUtils.splitLines
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.compat.hover
import at.hannibal2.skyhanni.utils.compat.suggest
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.tree.CommandNode
import net.minecraft.util.IChatComponent

@SkyHanniModule
object HelpCommand {

    private const val COMMANDS_PER_PAGE = 15
    private val messageId = ChatUtils.getUniqueMessageId()

    private fun createCommandEntry(command: CommandData, dispatcher: CommandDispatcher<Any?>): IChatComponent {
        val category = command.category
        val color = category.color
        val description = command.descriptor.splitLines(300).replace("§r", "§7")
        val categoryDescription = category.description.splitLines(200).replace("§r", "§7")
        val aliases = command.aliases

        val usage = if (command is BaseBrigadierBuilder) {
            val node = command.node
            val map = dispatcher.getSmartUsage(node, null)
            fun Map.Entry<CommandNode<Any?>, String>.format() = "§7 - §e/${node.name} $value"
            if (map.isEmpty()) null
            else buildString {
                appendLine()
                append("§7Usage:")
                if (map.size == 1) append(map.entries.first().format())
                else {
                    appendLine()
                    append(map.entries.joinToString("\n") { it.format() })
                }
            }
        } else null

        return TextHelper.text("§7 - $color${command.name}") {
            this.hover = TextHelper.multiline(
                "§e/${command.name}",
                if (aliases.isNotEmpty()) "§7Aliases: §e/${aliases.joinToString("§7, §e/")}" else null,
                if (description.isNotEmpty()) description.prependIndent("  ") else null,
                usage,
                "",
                "$color§l${category.categoryName}",
                categoryDescription.prependIndent("  "),
            )
            this.suggest = "/${command.name}"
        }
    }

    private fun showPage(page: Int, search: String, commands: List<CommandData>, dispatcher: CommandDispatcher<Any?>) {
        val filtered = commands.filter { cmd ->
            cmd.getAllNames().any { it.contains(search, ignoreCase = true) } ||
                cmd.descriptor.contains(search, ignoreCase = true)
        }

        val title = if (search.isBlank()) "SkyHanni Commands" else "SkyHanni Commands Matching: \"$search\""

        TextHelper.displayPaginatedList(
            title,
            filtered,
            chatLineId = messageId,
            emptyMessage = "No commands found.",
            currentPage = page,
            maxPerPage = COMMANDS_PER_PAGE,
        ) { createCommandEntry(it, dispatcher) }
    }

    fun onCommand(args: Array<String>, commands: List<CommandData>, dispatcher: CommandDispatcher<Any?>) {
        val page: Int
        val search: String
        if (args.firstOrNull() == "-p") {
            page = args.getOrNull(1)?.toIntOrNull() ?: 1
            search = args.drop(2).joinToString(" ")
        } else {
            page = 1
            search = args.joinToString(" ")
        }
        showPage(page, search, commands, dispatcher)
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shcommands") {
            description = "Shows this list"
            aliases = listOf("shhelp", "shcommand", "shcmd", "shc")
            legacyCallbackArgs { onCommand(it, event.commands, event.dispatcher) }
        }
    }
}
