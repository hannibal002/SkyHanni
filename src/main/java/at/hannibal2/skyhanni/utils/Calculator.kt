package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import com.notkamui.keval.Keval

@SkyHanniModule
object Calculator {

    fun calculateOrNull(input: String): Double? {
        return runCatching { Keval.eval(input) }.getOrNull()
    }

    private fun command(expr: String) = ChatUtils.chat(
        calculateOrNull(expr)?.let { result ->
            "$expr = §a$result"
        } ?: "§cFailed to calculate $expr"
    )

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shcalc") {
            category = CommandCategory.USERS_ACTIVE
            description = "Calculates a math expression"

            arg("expr", BrigadierArguments.greedyString()) { expr ->
                callback { command(getArg(expr)) }
            }
        }
    }
}
