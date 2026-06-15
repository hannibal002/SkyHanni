package at.hannibal2.skyhanni.config.commands.brigadier

import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils.dynamicSuggestionProvider
import at.hannibal2.skyhanni.features.commands.tabcomplete.PlayerCategory
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource

class PlayerSuggestions private constructor(
    private val sequence: Sequence<String>,
) {

    fun getPlayers(): List<String> =
        sequence.distinct().toList()

    fun toBrigadier(): SuggestionProvider<FabricClientCommandSource> {
        return dynamicSuggestionProvider { getPlayers() }
    }

    class Builder {
        private var seq: Sequence<String> = emptySequence()

        fun include(vararg categories: PlayerCategory) = apply {
            seq += categories.asSequence().flatMap { it.usernames() }
        }

        fun include(vararg players: String) = apply {
            seq += players.asSequence()
        }

        fun exclude(vararg categories: PlayerCategory) = apply {
            val excluded = categories.asSequence()
                .flatMap { it.usernames() }
                .toSet()

            seq = seq.filterNot { it in excluded }
        }

        fun exclude(vararg players: String) = apply {
            val excluded = players.toSet()
            seq = seq.filterNot { it in excluded }
        }

        fun filter(predicate: (String) -> Boolean) = apply {
            seq = seq.filter(predicate)
        }

        fun filterNot(predicate: (String) -> Boolean) = apply {
            seq = seq.filterNot(predicate)
        }

        fun build(): PlayerSuggestions {
            return PlayerSuggestions(seq)
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
