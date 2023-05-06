package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.test.CommandsSendToServerLogger

fun sendChatMessage(message: String) {
    CommandsSendToServerLogger.logCommandsToServer(message)
}