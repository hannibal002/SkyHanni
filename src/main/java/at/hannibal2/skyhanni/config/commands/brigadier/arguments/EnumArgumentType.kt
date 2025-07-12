package at.hannibal2.skyhanni.config.commands.brigadier.arguments

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
        val input = reader.readUnquotedString()
        val entry = mapping.entries.find { (string, _) -> string.equals(input, true) }
        return entry?.value ?: throw invalidValueException.createWithContext(reader, input)
    }

    override fun <S : Any?> listSuggestions(context: CommandContext<S>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        val string = builder.remainingLowerCase
        for (enum in mapping.keys) {
            if (enum.startsWith(string, true)) builder.suggest(enum)
        }
        return builder.buildFuture()
    }

    override fun getExamples(): Collection<String> = mapping.keys

    companion object {
        fun <E : Enum<E>> create(clazz: Class<E>, toString: (E) -> String): EnumArgumentType<E> {
            return EnumArgumentType(clazz, toString)
        }

        /**
         * To use enum name arguments do `EnumArgumentType.name<Enum>()`
         */
        inline fun <reified E : Enum<E>> name(): EnumArgumentType<E> {
            return create(E::class.java) { it.name }
        }

        /**
         * To use enum lowercase name arguments do `EnumArgumentType.lowercase<Enum>()`
         */
        inline fun <reified E : Enum<E>> lowercase(): EnumArgumentType<E> {
            return create(E::class.java) { it.name.lowercase() }
        }

        /** The string representation of the enum should not change during runtime. */
        inline fun <reified E : Enum<E>> custom(noinline toString: (E) -> String): EnumArgumentType<E> {
            return create(E::class.java, toString)
        }
    }
}
