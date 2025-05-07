package at.hannibal2.skyhanni.config.commands.brigadier

import com.mojang.brigadier.context.CommandContext

class ArgContext(val context: CommandContext<*>) {

    fun <T> getArg(argument: BrigadierArgument<T>): T = context.getArgument(argument.argumentName, argument.clazz)
    fun <T> get(argument: BrigadierArgument<T>): T = getArg(argument)
    operator fun <T> invoke(argument: BrigadierArgument<T>): T = getArg(argument)

    inline fun <reified T> getArgByName(name: String): T = context.getArgument(name, T::class.java)

}
