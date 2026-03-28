package at.hannibal2.skyhanni.config.commands.brigadier

import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource

class ArgContext(val context: CommandContext<FabricClientCommandSource>) {

    fun <T> getArg(argument: BrigadierArgument<T>): T = context.getArgument(argument.argumentName, argument.clazz)
    fun <T> get(argument: BrigadierArgument<T>): T = getArg(argument)
    operator fun <T> invoke(argument: BrigadierArgument<T>): T = getArg(argument)

    inline fun <reified T> getArgByName(name: String): T = context.getArgument(name, T::class.java)
}
