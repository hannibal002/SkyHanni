package at.hannibal2.skyhanni.config.commands.brigadier

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import java.util.concurrent.CompletableFuture

class PlayerSuggestions private constructor(
    private val include: Set<PlayerCategory>,
    private val exclude: Set<PlayerCategory>,
    private val customFilter: (String) -> Boolean,
) : SuggestionProvider<FabricClientCommandSource> {

    private val cached: List<String> by lazy {
        val excludedNames = exclude
            .asSequence()
            .flatMap { it.usernames() }
            .toSet()

        var result = include
            .asSequence()
            .flatMap { it.usernames() }
            .filterNot { it in excludedNames }
            .filter(customFilter)

        result = result.distinct()

        result.toList()
    }

    fun usernames(): List<String> = cached

    override fun getSuggestions(
        context: CommandContext<FabricClientCommandSource>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {

        val remaining = builder.remainingLowerCase

        cached
            .asSequence()
            .filter { it.lowercase().startsWith(remaining) }
            .forEach(builder::suggest)

        return builder.buildFuture()
    }

    class Builder {
        private val include = mutableSetOf<PlayerCategory>()
        private val exclude = mutableSetOf<PlayerCategory>()

        private var filter: (String) -> Boolean = { true }

        fun include(vararg categories: PlayerCategory) = apply {
            include.addAll(categories)
        }

        fun exclude(vararg categories: PlayerCategory) = apply {
            exclude.addAll(categories)
        }

        fun filter(predicate: (String) -> Boolean) = apply {
            val old = this.filter
            this.filter = { old(it) && predicate(it) }
        }

        fun filterNot(vararg categories: PlayerCategory) = apply {
            val excludedNames = categories
                .asSequence()
                .flatMap { it.usernames() }
                .toSet()

            filter { it !in excludedNames }
        }

        fun build(): PlayerSuggestions {
            return PlayerSuggestions(
                include = include,
                exclude = exclude,
                customFilter = filter,
            )
        }
    }

    companion object {
        fun builder(): Builder = Builder()

        fun all(): PlayerSuggestions =
            builder()
                .include(*PlayerCategory.entries.toTypedArray())
                .build()

        fun builder(block: Builder.() -> Unit): PlayerSuggestions {
            return Builder().apply(block).build()
        }
    }
}
