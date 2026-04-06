package at.hannibal2.skyhanni.config.commands

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.config.commands.CommandsRegistry.addToRegister
import at.hannibal2.skyhanni.config.commands.CommandsRegistry.hasUniqueName
import at.hannibal2.skyhanni.config.commands.brigadier.BaseBrigadierBuilder
import at.hannibal2.skyhanni.config.commands.brigadier.CommandData
import com.mojang.brigadier.CommandDispatcher
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

@PrimaryFunction("onCommandRegistration")
class CommandRegistrationEvent(
    val dispatcher: CommandDispatcher<FabricClientCommandSource>,
) : SkyHanniEvent() {
    private val builders = mutableListOf<CommandData>()

    val commands: List<CommandData> get() = builders

    fun registerBrigadier(name: String, builder: BaseBrigadierBuilder.() -> Unit) {
        val command = BaseBrigadierBuilder(name).apply(builder)
        command.hasUniqueName(builders)
        command.checkDescriptionAndCategory()
        command.addToRegister(dispatcher, builders)
    }

    private fun CommandData.checkDescriptionAndCategory() {
        require(descriptor.isNotEmpty() || category in CommandCategory.developmentCategories) {
            "The command '$name' has no required description"
        }
    }
}
