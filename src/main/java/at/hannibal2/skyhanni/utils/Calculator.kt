package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import com.notkamui.keval.keval

@SkyHanniModule
object Calculator {

    fun calculateOrNull(input: String): Double? {
        return runCatching {
            input.keval {
                // taken so kindly from skyocean
                includeDefault()

                constant {
                    name = "s"
                    value = 64.0
                }
                constant {
                    name = "e"
                    value = 160.0
                }
                constant {
                    name = "k"
                    value = 1000.0
                }
                constant {
                    name = "m"
                    value = 1_000_000.0
                }
                constant {
                    name = "b"
                    value = 1_000_000_000.0
                }
                constant {
                    name = "t"
                    value = 1_000_000_000_000.0
                }
            }
        }.getOrNull()
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shcalc") {
            category = CommandCategory.USERS_ACTIVE
            description = "Calculates a math expression"

            argCallback("expr", BrigadierArguments.greedyString()) { expr ->
                ChatUtils.chat(calculateOrNull(expr)?.let { result ->
                    "$expr = §a${result.addSeparators()}"
                } ?: "§cFailed to calculate $expr")

            }
        }
    }
}
