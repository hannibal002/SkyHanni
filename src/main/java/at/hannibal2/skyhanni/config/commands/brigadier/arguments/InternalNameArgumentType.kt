package at.hannibal2.skyhanni.config.commands.brigadier.arguments

import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils.escapeDoubleQuote
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils.readGreedyString
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils.readOptionalDoubleQuotedString
import at.hannibal2.skyhanni.utils.NeuInternalName
import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import java.util.concurrent.CompletableFuture

private typealias ParsingFail = BrigadierUtils.ItemParsingFail

sealed class InternalNameArgumentType(
    val isGreedy: Boolean = false,
) : ArgumentType<NeuInternalName> {

    protected open val showWhenEmpty: Boolean = false

    private val unknownValueException = DynamicCommandExceptionType { input ->
        LiteralMessage("Unknown item '$input'.")
    }

    private val disallowedValueException = DynamicCommandExceptionType { input ->
        LiteralMessage("Disallowed item '$input'.")
    }

    private val emptyValueException = SimpleCommandExceptionType { "Empty item name provided." }

    override fun parse(reader: StringReader): NeuInternalName {
        val input = if (isGreedy) reader.readGreedyString().escapeDoubleQuote()
        else reader.readOptionalDoubleQuotedString()

        val result = BrigadierUtils.parseItem(input, isValidItem = ::isValidItem)
        return when (result) {
            is NeuInternalName -> result
            ParsingFail.DISALLOWED_ITEM -> throw disallowedValueException.createWithContext(reader, input)
            ParsingFail.UNKNOWN_ITEM -> throw unknownValueException.createWithContext(reader, input)
            ParsingFail.EMPTY -> throw emptyValueException.createWithContext(reader)
            else -> throw IllegalArgumentException("Unexpected item parsing result: $result")
        }
    }

    protected open fun isValidItem(item: NeuInternalName): Boolean = true

    private open class ItemName(isGreedy: Boolean) : InternalNameArgumentType(isGreedy) {
        override fun <S : Any?> listSuggestions(context: CommandContext<S>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
            return BrigadierUtils.parseItemNameTabComplete(
                builder.remainingLowerCase,
                builder,
                showWhenEmpty = showWhenEmpty,
                isGreedy = isGreedy,
                isValidItem = ::isValidItem,
            )
        }
    }

    private open class InternalName(isGreedy: Boolean) : InternalNameArgumentType(isGreedy) {
        override fun <S : Any?> listSuggestions(context: CommandContext<S>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
            return BrigadierUtils.parseInternalNameTabComplete(
                builder.remaining,
                builder,
                showWhenEmpty = showWhenEmpty,
                isGreedy = isGreedy,
                isValidItem = ::isValidItem,
            )
        }
    }

    @Suppress("unused")
    companion object {
        fun itemName(isGreedy: Boolean = false): InternalNameArgumentType = ItemName(isGreedy)

        fun itemName(
            showWhenEmpty: Boolean = false,
            isGreedy: Boolean = false,
            isValid: (NeuInternalName) -> Boolean,
        ): InternalNameArgumentType {
            return object : ItemName(isGreedy) {
                override val showWhenEmpty: Boolean = showWhenEmpty
                override fun isValidItem(item: NeuInternalName): Boolean = isValid(item)
            }
        }

        fun itemName(
            allowed: Collection<NeuInternalName>,
            showWhenEmpty: Boolean = false,
            isGreedy: Boolean = false,
        ): InternalNameArgumentType {
            val set = allowed.toSet()
            return itemName(showWhenEmpty, isGreedy) { it in set }
        }

        fun internalName(isGreedy: Boolean = false): InternalNameArgumentType = InternalName(isGreedy)

        fun internalName(
            showWhenEmpty: Boolean = false,
            isGreedy: Boolean = false,
            isValid: (NeuInternalName) -> Boolean,
        ): InternalNameArgumentType {
            return object : InternalName(isGreedy) {
                override val showWhenEmpty: Boolean = showWhenEmpty
                override fun isValidItem(item: NeuInternalName): Boolean = isValid(item)
            }
        }

        fun internalName(
            allowed: Collection<NeuInternalName>,
            showWhenEmpty: Boolean = false,
            isGreedy: Boolean = false,
        ): InternalNameArgumentType {
            val set = allowed.toSet()
            return internalName(showWhenEmpty, isGreedy) { it in set }
        }
    }
}
