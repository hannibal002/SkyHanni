package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CommandsSendToServerLogger {
    companion object {
        fun logCommandsToServer(command: String) {
            if (SkyHanniMod.feature.dev.debug.commandLogs) {
                Exception("command send to server: '$command'").printStackTrace()
            }
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "dev.commandLogs", "dev.debug.commandLogs")
    }
}