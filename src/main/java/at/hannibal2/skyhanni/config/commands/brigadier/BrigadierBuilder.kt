package at.hannibal2.skyhanni.config.commands.brigadier

import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils.isGreedy
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils.toSuggestionProvider
import at.hannibal2.skyhanni.utils.StringUtils.hasWhitespace
import at.hannibal2.skyhanni.utils.StringUtils.splitLastWhitespace
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.tree.CommandNode
//#if MC < 1.21
import net.minecraft.command.ICommand
//#endif

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

    //#if MC < 1.21
    override fun toCommand(dispatcher: CommandDispatcher<Any?>): ICommand = BrigadierCommand(this, dispatcher)
    //#endif
}

open class BrigadierBuilder<B : ArgumentBuilder<Any?, B>>(
    val builder: ArgumentBuilder<Any?, B>,
    private val hasGreedyArg: Boolean = false,
) {
    private fun checkGreedy() =
        require(!hasGreedyArg) { "Cannot add an argument/literal to a builder that has a greedy argument." }

    /** Executes the code block when the command is executed. */
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
        simpleCallback { block(emptyArray()) }
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
    fun literal(vararg names: String, action: LiteralCommandBuilder.() -> Unit) {
        checkGreedy()
        for (name in names) {
            if (name.hasWhitespace()) {
                val (prevLiteral, nextLiteral) = name.splitLastWhitespace()
                literal(prevLiteral) {
                    literal(nextLiteral) {
                        action(this)
                    }
                }
                continue
            }
            val builder = BrigadierBuilder(LiteralArgumentBuilder.literal(name))
            builder.action()
            this.builder.then(builder.builder)
        }
    }

    /**
     * Adds an argument to the command. If in the same string there are
     * different names separated by spaces, only the last name is used as
     * the name of the argument, and the previous ones are treated as literals.
     *
     * To get the value of the argument in the callback block, use the [ArgContext.getArg]
     * or [ArgContext.get] methods with the [BrigadierArgument] given by [arg], or use
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
        crossinline action: ArgumentCommandBuilder<T>.(BrigadierArgument<T>) -> Unit,
    ) = arg(name, argument, suggestions.toSuggestionProvider(), action)

    /** @see arg */
    inline fun <reified T> arg(
        name: String,
        argument: ArgumentType<T>,
        suggestions: SuggestionProvider<Any?>? = null,
        crossinline action: ArgumentCommandBuilder<T>.(BrigadierArgument<T>) -> Unit,
    ) {
        if (!name.hasWhitespace()) {
            internalArg(name, argument, suggestions) { action(BrigadierArgument.of(name)) }
            return
        }
        val (literalNames, argName) = name.splitLastWhitespace()
        literal(literalNames) {
            internalArg(argName, argument, suggestions) { action(BrigadierArgument.of<T>(argName)) }
        }
    }

    /**
     * Intended for internal use only. It's the same as other arg functions, but it
     * doesn't have the [BrigadierArgument] passed as a parameter. The reason for this method
     * existing is that all the other arg methods have to use reified types, which means that
     * they can't be used recursively.
     */
    fun <T> internalArg(
        name: String,
        argument: ArgumentType<T>,
        suggestions: SuggestionProvider<Any?>? = null,
        action: ArgumentCommandBuilder<T>.() -> Unit,
    ) {
        checkGreedy()
        if (name.hasWhitespace()) {
            val (prevLiteral, nextLiteral) = name.splitLastWhitespace()
            literal(prevLiteral) {
                internalArg(nextLiteral, argument, suggestions, action)
            }
            return
        }
        val isGreedy = argument.isGreedy()
        val builder = BrigadierBuilder(
            RequiredArgumentBuilder.argument<Any?, T>(name, argument).apply {
                if (suggestions != null) suggests(suggestions)
            },
            isGreedy,
        )
        builder.action()
        this.builder.then(builder.builder)
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
     * For args, the same applies; the only difference is that instead of giving a [BrigadierArgument] as a parameter,
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
