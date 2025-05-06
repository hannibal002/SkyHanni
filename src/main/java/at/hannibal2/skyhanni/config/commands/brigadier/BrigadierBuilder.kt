package at.hannibal2.skyhanni.config.commands.brigadier

import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils.toSuggestionProvider
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.tree.CommandNode
import net.minecraft.command.ICommand

typealias LiteralCommandBuilder = BrigadierBuilder<LiteralArgumentBuilder<Any?>>
typealias ArgumentCommandBuilder<T> = BrigadierBuilder<RequiredArgumentBuilder<Any?, T>>

class BaseBrigadierBuilder(override val name: String) : CommandData, BrigadierBuilder<LiteralArgumentBuilder<Any?>>(
    LiteralArgumentBuilder.literal<Any?>(name),
) {
    var description: String = ""
    override var aliases: List<String> = emptyList()
    override var category: CommandCategory = CommandCategory.MAIN

    override val descriptor: String
        get() = description

    lateinit var node: CommandNode<Any?>

    override fun toCommand(dispatcher: CommandDispatcher<Any?>): ICommand = BrigadierCommand(this, dispatcher)
}

open class BrigadierBuilder<B : ArgumentBuilder<Any?, B>>(
    val builder: ArgumentBuilder<Any?, B>,
) {
    /**
     * Executes the code block when the command is executed.
     */
    fun callback(block: ArgContext.() -> Unit) {
        this.builder.executes {
            block(ArgContext(it))
            1
        }
    }

    /** Alternative to [callback] when no arguments are needed. */
    fun simpleCallback(block: () -> Unit) {
        this.builder.executes {
            block()
            1
        }
    }

    /**
     * Callback method similar to the one used by Forge, where you are given
     * all arguments as an array.
     *
     * Usage of this method is discouraged, unless it's for compatibility with legacy code.
     */
    fun legacyCallbackArgs(block: (Array<String>) -> Unit) {
        argCallback("allArgs", BrigadierArguments.greedyString()) { allArgs ->
            block(allArgs.split(" ").toTypedArray())
        }
        callback { block(emptyArray()) }
    }

    /**
     * Adds a literal to the command. The different names given via vararg are
     * treated as aliases for the same literal.
     * If in the same string there are different names separated by spaces, it
     * is treated as a chain of literals.
     *
     * For example, the following usage:
     * ```kt
     * literal("first second") {
     *    // do something
     * }
     * ```
     * Is the same as this usage:
     * ```kt
     * literal("first") {
     *    literal("second") {
     *       // do something
     *    }
     * }
     * ```
     */
    fun literal(vararg names: String, action: LiteralCommandBuilder.() -> Unit): BrigadierBuilder<B> {
        for (name in names) {
            if (name.contains(" ")) {
                val builder = BrigadierBuilder(LiteralArgumentBuilder.literal(name.substringBefore(" ")))
                builder.literal(name.substringAfter(" "), action = action)
                this.builder.then(builder.builder)
                continue
            }
            val builder = BrigadierBuilder(LiteralArgumentBuilder.literal(name))
            builder.action()
            this.builder.then(builder.builder)
        }
        return this
    }

    /**
     * Adds an argument to the command. If in the same string there are
     * different names separated by spaces, only the last name is used as
     * the name of the argument, and the previous ones are treated as literals.
     *
     * To get the value of the argument in the callback block, use the [ArgContext.getArg]
     * or [ArgContext.get] methods with the [CommandArgument] given by [arg], or use
     * [ArgContext.getArgByName] if you want to use the argument name instead.
     *
     * Example usage:
     * ```kt
     * arg("input", BrigadierArguments.string()) { inputArg ->
     *    callback {
     *       val input = getArg(inputArg)
     *       ChatUtils.chat("Sent input: $input")
     *    }
     * }
     * ```
     */
    inline fun <reified T> arg(
        name: String,
        argument: ArgumentType<T>,
        suggestions: Collection<String>,
        crossinline action: ArgumentCommandBuilder<T>.(CommandArgument<T>) -> Unit,
    ): BrigadierBuilder<B> = arg(name, argument, suggestions.toSuggestionProvider(), action)


    /**
     * @see arg
     * */
    inline fun <reified T> arg(
        name: String,
        argument: ArgumentType<T>,
        suggestions: SuggestionProvider<Any?>? = null,
        crossinline action: ArgumentCommandBuilder<T>.(CommandArgument<T>) -> Unit,
    ): BrigadierBuilder<B> {
        if (!name.contains("  ")) {
            return internalArg(name, argument, suggestions) { action(CommandArgument(name, T::class.java)) }
        }
        val split = name.split(" ")
        val beforeArg = split.subList(0, split.size - 1).joinToString(" ")
        val argName = split.last()
        return internalArg(beforeArg, argument, suggestions) { action(CommandArgument(argName, T::class.java)) }
    }

    /**
     * Intended for internal use only. It's the same as other arg functions, but it
     * doesn't have the CommandArgument passed as a parameter. The reason for this method
     * existing is that all the other arg methods have to use reified types, which means that
     * they can't be used recursively.
     */
    fun <T> internalArg(
        name: String,
        argument: ArgumentType<T>,
        suggestions: SuggestionProvider<Any?>? = null,
        action: ArgumentCommandBuilder<T>.() -> Unit,
    ): BrigadierBuilder<B> {
        if (name.contains(" ")) {
            val builder = BrigadierBuilder(LiteralArgumentBuilder.literal(name.substringBefore(" ")))
            builder.internalArg(name.substringAfter(" "), argument, suggestions, action)
            this.builder.then(builder.builder)
            return this
        }
        val builder = BrigadierBuilder(
            RequiredArgumentBuilder.argument<Any?, T>(name, argument).apply {
                if (suggestions != null) suggests(suggestions)
            },
        )
        builder.action()
        this.builder.then(builder.builder)
        return this
    }

    /**
     * This function allows for the usage of a callback within a literal without having to
     * create a block for each one.
     *
     * For example, this usage of literalCallback
     * ```kt
     * literalCallback("test") {
     *     // do something
     * }
     * ```
     * is the same as this usage of literal and callback separately:
     * ```kt
     * literal("test") {
     *     callback {
     *        // do something
     *     }
     * }
     * ```
     */
    fun literalCallback(
        vararg names: String,
        block: ArgContext.() -> Unit,
    ) = literal(*names) { callback(block) }

    /**
     * This function allows for the usage of a callback within an argument without having to
     * create a block for each one.
     *
     * However, differently from [literalCallback]
     * For args, the same applies; the only difference is that instead of giving a CommandArgument<T> as a parameter,
     * it directly gives the value of the argument.
     *
     * For example, the following two usages are the same:
     *
     * ```kt
     * argCallback("input", BrigadierArguments.string()) { input ->
     *     ChatUtils.chat("Sent input: $input")
     * }
     * ```
     * ```kt
     * arg("input", BrigadierArguments.string()) { inputArg ->
     *    callback {
     *       val input = getArg(inputArg)
     *       ChatUtils.chat("Sent input: $input")
     *    }
     * }
     * ```
     */
    inline fun <reified T> argCallback(
        name: String,
        argument: ArgumentType<T>,
        suggestions: Collection<String>,
        crossinline block: ArgContext.(T) -> Unit,
    ) = arg(name, argument, suggestions) { callback { block(getArg(it)) } }

    /** @see argCallback */
    inline fun <reified T> argCallback(
        name: String,
        argument: ArgumentType<T>,
        suggestions: SuggestionProvider<Any?>? = null,
        crossinline callback: ArgContext.(T) -> Unit,
    ) = arg(name, argument, suggestions) { callback { callback(getArg(it)) } }


}
