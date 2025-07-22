package at.hannibal2.skyhanni.config.commands.brigadier.arguments

import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils.addOptionalEscaped
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils.addUnescaped
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils.escapeDoubleQuote
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils.readGreedyString
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils.readOptionalDoubleQuotedString
import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import java.util.concurrent.CompletableFuture

class EnumArgumentType<E : Enum<E>> private constructor(
    clazz: Class<E>,
    val isGreedy: Boolean = false,
    toString: (E) -> String,
) : ArgumentType<E> {

    private val mapping: Map<String, E> = clazz.enumConstants.associateBy { constant ->
        val string = toString(constant)
        require(string.none { it.isWhitespace() }) {
            "String representation of constant ${constant.name} of enum ${clazz.simpleName} contains whitespace: '$string'"
        }
        string
    }

    private val invalidValueException = DynamicCommandExceptionType { input ->
        LiteralMessage("Invalid value '$input'.")
    }

    override fun parse(reader: StringReader): E {
        val input = if (isGreedy) reader.readGreedyString().escapeDoubleQuote()
        else reader.readOptionalDoubleQuotedString()
        val entry = mapping.entries.find { (string, _) -> string.equals(input.replace(" ", "_"), true) }
        return entry?.value ?: throw invalidValueException.createWithContext(reader, input)
    }

    override fun <S : Any?> listSuggestions(context: CommandContext<S>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        val string = builder.remainingLowerCase
        val items = mutableListOf<String>()
        for (enum in mapping.keys) {
            val enumWithSpaces = enum.replace("_", " ")
            if (enumWithSpaces.startsWith(string, true)) items.add(enumWithSpaces)
        }
        if (isGreedy) builder.addUnescaped(items) else builder.addOptionalEscaped(items)
        return builder.buildFuture()
    }

    override fun getExamples(): Collection<String> = mapping.keys

    companion object {
        fun <E : Enum<E>> create(clazz: Class<E>, isGreedy: Boolean = false, toString: (E) -> String): EnumArgumentType<E> {
            return EnumArgumentType(clazz, isGreedy, toString)
        }

        /**
         * To use enum name arguments do `EnumArgumentType.name<Enum>()`
         */
        inline fun <reified E : Enum<E>> name(isGreedy: Boolean = false): EnumArgumentType<E> {
            return create(E::class.java, isGreedy) { it.name }
        }

        /**
         * To use enum lowercase name arguments do `EnumArgumentType.lowercase<Enum>()`
         */
        inline fun <reified E : Enum<E>> lowercase(isGreedy: Boolean = false): EnumArgumentType<E> {
            return create(E::class.java, isGreedy) { it.name.lowercase() }
        }

        /** The string representation of the enum should not change during runtime. */
        inline fun <reified E : Enum<E>> custom(noinline toString: (E) -> String, isGreedy: Boolean = false): EnumArgumentType<E> {
            return create(E::class.java, isGreedy, toString)
        }
    }
}
