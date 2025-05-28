package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.data.BossbarData
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor

@SkyHanniModule
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
