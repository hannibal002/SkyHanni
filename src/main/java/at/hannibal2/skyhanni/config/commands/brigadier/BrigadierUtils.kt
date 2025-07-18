package at.hannibal2.skyhanni.config.commands.brigadier

import at.hannibal2.skyhanni.config.commands.brigadier.arguments.InternalNameArgumentType
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.StringUtils.hasWhitespace
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import java.util.concurrent.CompletableFuture

object BrigadierUtils {

    const val DOUBLE_QUOTE = '"'
    private const val SINGLE_QUOTE = '\''

    // Add greedy arguments here
    fun <T> ArgumentType<T>.isGreedy(): Boolean {
        return when (this) {
            is StringArgumentType -> this.type == StringArgumentType.StringType.GREEDY_PHRASE
            is InternalNameArgumentType -> this.isGreedy
            else -> false
        }
    }

    fun Collection<String>.toSuggestionProvider() = SuggestionProvider<Any?> { _, builder ->
        for (s in this) {
            if (s.startsWith(builder.remainingLowerCase)) {
                builder.suggest(s)
            }
        }
        builder.buildFuture()
    }

    private fun isCharAllowed(c: Char): Boolean = StringReader.isAllowedInUnquotedString(c) || c == SINGLE_QUOTE

    /** The same as [StringReader.readString], except it doesn't accept escaping with `'`. */
    fun StringReader.readOptionalDoubleQuotedString(): String {
        if (!canRead()) return ""
        return if (peek() == DOUBLE_QUOTE) {
            skip()
            readStringUntil(DOUBLE_QUOTE)
        } else {
            val start = cursor
            while (canRead() && isCharAllowed(peek())) {
                skip()
            }
            return string.substring(start, cursor)
        }
    }

    /** Returns the remaining text in the [StringReader] and sets the cursor at the end */
    fun StringReader.readGreedyString(): String {
        val text = remaining
        cursor = totalLength
        return text
    }

    /** The same as [StringReader.readQuotedString], except it doesn't accept escaping with `'`. */
    fun StringReader.readDoubleQuotedString(): String {
        if (!canRead()) return ""
        if (peek() != DOUBLE_QUOTE) throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedStartOfQuote().createWithContext(this)
        skip()
        return readStringUntil(DOUBLE_QUOTE)
    }

    fun String.escapeDoubleQuote(): String {
        if (firstOrNull() != DOUBLE_QUOTE) return this
        return substring(1, lastIndex - 1)
    }

    enum class ItemParsingFail {
        UNKNOWN_ITEM,
        DISALLOWED_ITEM,
        EMPTY,
    }

    /** Parses an item (both internal name and item name) into an NeuInternalName. If it fails, it returns an ItemParsingFail instead */
    fun parseItem(
        input: String,
        aliases: Map<String, NeuInternalName> = NeuItems.commonItemAliases.global,
        isValidItem: (NeuInternalName) -> Boolean = { true },
    ): Any {
        if (input.isBlank()) return ItemParsingFail.EMPTY
        val withSpaces = input.replace("_", " ")

        fun NeuInternalName.handleItem(): Any = when {
            !isKnownItem() -> ItemParsingFail.UNKNOWN_ITEM
            !isValidItem(this) -> ItemParsingFail.DISALLOWED_ITEM
            else -> this
        }

        return aliases[withSpaces]?.handleItem()
            ?: NeuInternalName.fromItemNameOrInternalName(withSpaces).handleItem()
    }

    fun SuggestionsBuilder.addOptionalEscaped(
        collection: Collection<String>,
    ): SuggestionsBuilder {
        if (collection.isEmpty()) return this
        val input = remainingLowerCase
        val isEscaped = input.firstOrNull() == DOUBLE_QUOTE
        //#if MC < 1.21
        val escaped = if (isEscaped) input.drop(1) else input
        val lastWhitespace = escaped.lastIndexOf(' ')
        for (string in collection) {
            if (lastWhitespace == -1) {
                if (isEscaped || string.hasWhitespace()) suggest("$DOUBLE_QUOTE$string$DOUBLE_QUOTE")
                else suggest(string)
            } else {
                val suggestion = string.substring(lastWhitespace + 1)
                if (suggestion.isBlank()) suggest("$DOUBLE_QUOTE")
                else suggest("$suggestion$DOUBLE_QUOTE")
            }
        }
        //#else
        //$$ for (string in collection) {
        //$$     if (isEscaped || string.hasWhitespace()) suggest("$DOUBLE_QUOTE$string$DOUBLE_QUOTE")
        //$$     else suggest(string)
        //$$ }
        //#endif
        return this
    }

    fun SuggestionsBuilder.addEscaped(
        collection: Collection<String>,
    ): SuggestionsBuilder {
        if (collection.isEmpty()) return this
        val input = remainingLowerCase
        //#if MC < 1.21
        val escaped = input.drop(1)
        val lastWhitespace = escaped.lastIndexOf(' ')
        for (string in collection) {
            if (lastWhitespace == -1) {
                suggest("$DOUBLE_QUOTE$string$DOUBLE_QUOTE")
            } else {
                val suggestion = string.substring(lastWhitespace + 1)
                if (suggestion.isBlank()) suggest("$DOUBLE_QUOTE")
                else suggest("$suggestion$DOUBLE_QUOTE")
            }
        }
        //#else
        //$$ for (string in collection) suggest("$DOUBLE_QUOTE$string$DOUBLE_QUOTE")
        //#endif
        return this
    }

    // TODO: add support for 1.21
    fun SuggestionsBuilder.addUnescaped(
        collection: Collection<String>,
    ): SuggestionsBuilder {
        if (collection.isEmpty()) return this
        val input = remainingLowerCase
        //#if MC < 1.21
        val isEscaped = input.firstOrNull() == DOUBLE_QUOTE
        val escaped = if (isEscaped) input.drop(1) else input
        val lastWhitespace = escaped.lastIndexOf(' ')
        for (string in collection) {
            if (lastWhitespace == -1) {
                if (input == string) continue
                suggest(string)
            } else {
                val suggestion = string.substring(lastWhitespace + 1) + if (isEscaped) DOUBLE_QUOTE else ""
                if (suggestion.isNotBlank()) suggest(suggestion)
            }
        }
        //#else
        //$$ for (string in collection) suggest(string)
        //#endif
        return this
    }

    fun parseItemNameTabComplete(
        input: String,
        builder: SuggestionsBuilder,
        limit: Int = 200,
        showWhenEmpty: Boolean = false,
        isGreedy: Boolean = false,
        isValidItem: (NeuInternalName) -> Boolean,
    ): CompletableFuture<Suggestions> {
        if (input.isEmpty() && !showWhenEmpty) return builder.buildFuture()
        val unEscaped = if (input.firstOrNull() == DOUBLE_QUOTE) input.drop(1) else input
        if (unEscaped.isBlank() && !showWhenEmpty) return builder.buildFuture()

        val lowercaseStart = unEscaped.replace("_", " ")
        val items = NeuItems.findItemNameStartingWithWithoutNPCs(lowercaseStart, isValidItem).take(limit)

        if (isGreedy) builder.addUnescaped(items) else builder.addOptionalEscaped(items)
        return builder.buildFuture()
    }

    fun parseInternalNameTabComplete(
        input: String,
        builder: SuggestionsBuilder,
        limit: Int = 200,
        showWhenEmpty: Boolean = false,
        isGreedy: Boolean = false,
        isValidItem: (NeuInternalName) -> Boolean,
    ): CompletableFuture<Suggestions> {
        if (input.isEmpty() && !showWhenEmpty) return builder.buildFuture()
        val unEscaped = if (input.firstOrNull() == DOUBLE_QUOTE) input.drop(1) else input
        if (unEscaped.isBlank() && !showWhenEmpty) return builder.buildFuture()

        val start = unEscaped.replace(" ", "_").uppercase()
        val items = NeuItems.findInternalNameStartingWithWithoutNPCs(start, isValidItem).take(limit)

        if (isGreedy) builder.addUnescaped(items) else builder.addOptionalEscaped(items)
        return builder.buildFuture()
    }
}
