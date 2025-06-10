package at.hannibal2.skyhanni.utils.chat

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils

@SkyHanniModule
object ChatClickActionManager {

    private val actions = mutableMapOf<String, ClickableAction>()

    fun createAction(onClick: () -> Any, expiresAt: SimpleTimeMark, oneTime: Boolean = true): String {
        val token = StringUtils.generateRandomId()
        actions[token] = ClickableAction(onClick, oneTime, expiresAt)
        return token
    }

    fun onCommand(id: String) {
        actions[id]?.apply {
            if (expiresAt.isInPast()) {
                actions.remove(id)
                return
            }
            onClick()
            if (oneTime) {
                actions.remove(id)
            }
        }
    }

    class ClickableAction(
        val onClick: () -> Any,
        val oneTime: Boolean = true,
        val expiresAt: SimpleTimeMark = SimpleTimeMark.farFuture(),
    )

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shaction") {
            description = "Internal command for chat click actions"
            category = CommandCategory.INTERNAL
            arg("id", BrigadierArguments.string()) {
                callback { onCommand(getArg(it)) }
            }
            simpleCallback {
                ChatUtils.userError("This command is an internal command. There is no need to manually run this command")
            }
        }
    }
}
