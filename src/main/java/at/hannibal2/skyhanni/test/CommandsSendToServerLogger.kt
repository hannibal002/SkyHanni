package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod

class CommandsSendToServerLogger {
    companion object {
        fun logCommandsToServer(command: String) {
            if (SkyHanniMod.feature.dev.commandLogs) {
                Exception("command send to server: '$command'").printStackTrace()
            }
        }
    }
}