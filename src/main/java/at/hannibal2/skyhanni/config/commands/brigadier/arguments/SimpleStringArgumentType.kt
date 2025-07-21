package at.hannibal2.skyhanni.config.commands.brigadier.arguments

import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils.addOptionalEscaped
import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import java.util.concurrent.CompletableFuture

class SimpleStringArgumentType<T : Any>(
    map: Map<String, T>,
) : ArgumentType<T> {

    private val map: Map<String, T> = map.mapKeys { it.key.lowercase() }

    private val invalidValueException = DynamicCommandExceptionType { input ->
        LiteralMessage("Invalid value '$input'.")
    }

    override fun parse(reader: StringReader): T {
        val input = reader.readString().lowercase()
        return map[input] ?: throw invalidValueException.createWithContext(reader, input)
    }

    override fun <S : Any> listSuggestions(context: CommandContext<S>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        return builder.addOptionalEscaped(map.keys).buildFuture()
    }

    override fun getExamples(): Collection<String> = map.keys
}
