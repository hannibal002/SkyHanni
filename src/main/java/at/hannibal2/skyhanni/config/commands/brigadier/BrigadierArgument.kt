package at.hannibal2.skyhanni.config.commands.brigadier

data class BrigadierArgument<T>(val argumentName: String, val clazz: Class<T>) {
    fun get(context: ArgContext): T = context.getArg(this)
    operator fun invoke(context: ArgContext) = get(context)

    companion object {
        inline fun <reified T> of(argumentName: String) = BrigadierArgument(argumentName, T::class.java)
    }

}
