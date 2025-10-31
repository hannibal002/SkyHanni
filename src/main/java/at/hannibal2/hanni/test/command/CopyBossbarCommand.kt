package at.hannibal2.hanni.test.command

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.commands.CommandCategory
import at.hannibal2.hanni.config.commands.CommandRegistrationEvent
import at.hannibal2.hanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.hanni.data.BossbarData
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.OSUtils
import at.hannibal2.hanni.utils.StringUtils.removeColor

@HanniModule
object CopyBossbarCommand {
    fun command(noFormattingCodes: Boolean) {
        val bossbarName = if (noFormattingCodes) BossbarData.getBossbar().removeColor() else BossbarData.getBossbar()
        val status = if (noFormattingCodes) "without" else "with"
        if (bossbarName.isBlank()) {
            ChatUtils.chat("Boss bar appears to be blank.")
        } else {
            OSUtils.copyToClipboard(bossbarName)
            ChatUtils.chat("Boss bar name copied to clipboard $status formatting codes!")
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shcopybossbar") {
            description = "Copies the action bar to the clipboard, including formatting codes"
            category = CommandCategory.DEVELOPER_DEBUG
            arg("nocolor", BrigadierArguments.bool()) { noColor ->
                callback { command(getArg(noColor)) }
            }
            simpleCallback { command(false) }
        }
    }
}
