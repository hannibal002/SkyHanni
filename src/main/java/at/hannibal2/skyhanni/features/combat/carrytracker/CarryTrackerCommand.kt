package at.hannibal2.skyhanni.features.combat.carrytracker

import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.LiteralCommandBuilder
import at.hannibal2.skyhanni.config.commands.brigadier.PlayerSuggestions
import at.hannibal2.skyhanni.features.commands.tabcomplete.PlayerNameSource
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils

object CarryTrackerCommand {

    fun registerCarryCommand(event: CommandRegistrationEvent) {
        event.registerBrigadier("shcarry") {
            description = "Keep track of carries you do."
            category = CommandCategory.USERS_ACTIVE
            literal("add") { add() }
            literal("remove") { remove() }
            literal("updateDone") { updateDone() }
            literal("updatePaid") { updatePaid() }
            literal("price") { price() }
            literalCallback("clearAll") {
                CarryTracker.clearAll()
            }
            simpleCallback {
                showHelpUserError()
            }
        }
    }

    private fun LiteralCommandBuilder.add() {
        arg(
            "player",
            BrigadierArguments.word(),
            PlayerSuggestions.builder { includeAllSources() },
        ) { player ->
            arg(
                "type",
                BrigadierArguments.word(),
                CarryTracker.getCarryTypeIds(),
            ) { type ->
                arg(
                    "amount",
                    BrigadierArguments.integer(),
                    listOf("1", "10", "30", "160", "200"),
                ) { amount ->
                    callback {
                        CarryTracker.addCarry(getArg(player), getArg(type), getArg(amount))
                    }
                }
                simpleCallback {
                    showHelpUserError()
                }
            }
            simpleCallback {
                showHelpUserError()
            }
        }
        simpleCallback {
            showHelpUserError()
        }
    }

    private fun LiteralCommandBuilder.remove() {
        arg(
            "player",
            BrigadierArguments.word(),
            PlayerSuggestions.builder { include(PlayerNameSource.CARRY_CUSTOMER) },
        ) { player ->
            arg(
                "type",
                BrigadierArguments.word(),
                CarryTracker.getCarryTypeIds(),
            ) { type ->
                arg(
                    "amount",
                    BrigadierArguments.integer(),
                    listOf("1", "10", "30", "160", "200"),
                ) { amount ->
                    callback {
                        CarryTracker.updateTotal(getArg(player), getArg(type), -getArg(amount))
                    }
                }
                callback {
                    CarryTracker.removeCarry(getArg(player), getArg(type))
                }
            }
            callback {
                CarryTracker.removeCustomer(getArg(player))
            }
        }
        simpleCallback {
            showHelpUserError()
        }
    }

    private fun LiteralCommandBuilder.updateDone() {
        arg(
            "player",
            BrigadierArguments.word(),
            PlayerSuggestions.builder { include(PlayerNameSource.CARRY_CUSTOMER) },
        ) { player ->
            arg(
                "type",
                BrigadierArguments.word(),
                CarryTracker.getCarryTypeIds(),
            ) { type ->
                arg(
                    "amount",
                    BrigadierArguments.integer(),
                    listOf("1", "5", "10", "-1", "-5", "-10"),
                ) { amount ->
                    callback {
                        CarryTracker.updateDone(getArg(player), getArg(type), getArg(amount))
                    }
                }
                simpleCallback {
                    showHelpUserError()
                }
            }
            simpleCallback {
                showHelpUserError()
            }
        }
        simpleCallback {
            showHelpUserError()
        }
    }

    private fun LiteralCommandBuilder.updatePaid() {
        arg(
            "player",
            BrigadierArguments.word(),
            PlayerSuggestions.builder { include(PlayerNameSource.CARRY_CUSTOMER) },
        ) { player ->
            arg(
                "coins",
                BrigadierArguments.word(),
                listOf("1m", "5m", "10m", "-1m", "-5m", "-10m"),
            ) { coins ->
                callback {
                    CarryTracker.updatePaid(getArg(player), getArg(coins))
                }
            }
            simpleCallback {
                showHelpUserError()
            }
        }
        simpleCallback {
            showHelpUserError()
        }
    }

    private fun LiteralCommandBuilder.price() {
        literal("list") {
            argCallback(
                "page",
                BrigadierArguments.integer(),
            ) { page ->
                CarryTracker.listPrices(page)
            }
            simpleCallback {
                CarryTracker.listPrices()
            }
        }
        literal("set") {
            arg(
                "type",
                BrigadierArguments.word(),
                CarryTracker.getCarryTypeIds(),
            ) { type ->
                arg(
                    "price",
                    BrigadierArguments.word(),
                    listOf("1m", "2m", "3m", "4m", "5m"),
                ) { price ->
                    callback {
                        CarryTracker.setPrice(getArg(type), getArg(price))
                    }
                }
                simpleCallback {
                    showHelpUserError()
                }
            }
        }
        literal("delete") {
            argCallback(
                "type",
                BrigadierArguments.word(),
                CarryTracker.getCarryTypeIds(),
            ) { type ->
                CarryTracker.deletePrice(type)
            }
            simpleCallback {
                showHelpUserError()
            }
        }
        literalCallback("deleteAll") {
            CarryTracker.deleteAllPrices()
        }
        simpleCallback {
            showHelpUserError()
        }
    }

    private fun showHelpUserError() {
        ChatUtils.userError(
            "Usage:\n" +
                "  §c/shcarry add <player> [type] [amount]\n" +
                "  §c/shcarry remove <player> [type] [amount]\n" +
                "  §c/shcarry updateDone <player> <type> <amount>\n" +
                "  §c/shcarry updatePaid <player> <coins>\n" +
                "  §c/shcarry price list\n" +
                "  §c/shcarry price set <type> <price>\n" +
                "  §c/shcarry price delete <type>\n" +
                "  §c/shcarry price deleteAll\n" +
                "  §c/shcarry clearAll",
            replaceSameMessage = true,
        )
    }
}
