package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LocationUtils

@SkyHanniModule
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
