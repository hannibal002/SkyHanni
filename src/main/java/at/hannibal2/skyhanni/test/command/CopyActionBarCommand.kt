package at.hannibal2.hanni.test.command

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.commands.CommandCategory
import at.hannibal2.hanni.config.commands.CommandRegistrationEvent
import at.hannibal2.hanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.hanni.data.ActionBarData
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.OSUtils
import at.hannibal2.hanni.utils.StringUtils.removeColor

@HanniModule
object CopyActionBarCommand {
    private fun command(noFormattingCodes: Boolean) {
        val status = if (noFormattingCodes) "without" else "with"

        var actionBar = ActionBarData.getActionBar()
        if (noFormattingCodes) actionBar = actionBar.removeColor()

        OSUtils.copyToClipboard(actionBar)
        ChatUtils.chat("Action bar name copied to clipboard $status formatting codes!")
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shcopyactionbar") {
            description = "Copies the action bar to the clipboard, including formatting codes"
            category = CommandCategory.DEVELOPER_DEBUG
            arg("nocolor", BrigadierArguments.bool()) { noColor ->
                callback { command(getArg(noColor)) }
            }
            simpleCallback { command(false) }
        }
    }
}
