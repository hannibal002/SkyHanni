package at.hannibal2.hanni.features.commands

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.commands.CommandCategory
import at.hannibal2.hanni.config.commands.CommandRegistrationEvent
import at.hannibal2.hanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.LocationUtils

@HanniModule
object SendCoordinatedCommand {

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("sendcoords") {
            description = "Sends your current coordinates in chat"
            category = CommandCategory.USERS_ACTIVE
            argCallback("message", BrigadierArguments.greedyString()) { message ->
                ChatUtils.sendMessageToServer(getCoordinates() + " $message")
            }
            callback {
                ChatUtils.sendMessageToServer(getCoordinates())
            }
        }
    }

    private fun getCoordinates(): String {
        val location = LocationUtils.playerLocation()
        val x = location.x.toInt()
        val y = location.y.toInt()
        val z = location.z.toInt()
        return "x: $x, y: $y, z: $z"
    }
}
