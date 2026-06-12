package at.hannibal2.skyhanni.config.commands.brigadier

import at.hannibal2.skyhanni.features.commands.tabcomplete.PlayerCategory
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource

class PlayerSuggestions private constructor(
    private val include: Set<PlayerCategory>,
    private val exclude: Set<PlayerCategory>,
    private val filter: (String) -> Boolean
) {

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
            val old = filter
            filter = { old(it) && predicate(it) }
        }

        fun filterNot(predicate: (String) -> Boolean) = apply {
            val old = filter
            filter = { old(it) && !predicate(it) }
        }

        fun build(): PlayerSuggestions {
            return PlayerSuggestions(include, exclude, filter)
        }
    }

    private val cached: List<String> by lazy {
        val excludedNames = exclude
            .flatMap(PlayerCategory::usernames)
            .toSet()

        include
            .flatMap(PlayerCategory::usernames)
            .filterNot(excludedNames::contains)
            .filter(filter)
            .toList()
    }

    fun toBrigadier(): SuggestionProvider<FabricClientCommandSource> {
        return SuggestionProvider { _, builder ->

            val remaining = builder.remainingLowerCase

            cached
                .filter { it.lowercase().startsWith(remaining) }
                .forEach { builder.suggest(it) }

            builder.buildFuture()
        }
    }

    companion object {
        fun build(block: Builder.() -> Unit): SuggestionProvider<FabricClientCommandSource> {
            return Builder()
                .apply(block)
                .build()
                .toBrigadier()
        }
    }
}
