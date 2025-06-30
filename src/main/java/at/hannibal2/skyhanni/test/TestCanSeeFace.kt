package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object TestCanSeeFace {

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {

    }

}
