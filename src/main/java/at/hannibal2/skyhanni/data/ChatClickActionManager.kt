package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.StringUtils

object ChatClickActionManager {

    private val actions = mutableListOf<ClickableAction>()

    fun oneTimeClick(message: String, onClick: () -> Any) {
        val action = ClickableAction(StringUtils.generateRandomId(), message, onClick)
        actions.add(action)
        action.sendToChat()
    }

    private fun ClickableAction.sendToChat() {
        ChatUtils.clickableChat(message, "shaction $token", prefix = false)
    }

    fun onCommand(args: Array<String>) {
        if (args.size == 1) {
            getActionByToken(args[0])?.runAction()
        }
    }

    private fun ClickableAction.runAction() {
        onClick()
        if (oneTime) {
            actions.remove(this)
        }
    }

    private fun getActionByToken(token: String) = actions.find { it.token == token }

    class ClickableAction(val token: String, val message: String, val onClick: () -> Any, val oneTime: Boolean = true)
}
