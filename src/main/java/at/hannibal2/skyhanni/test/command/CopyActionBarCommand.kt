package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.data.ActionBarData
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor

@SkyHanniModule
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
