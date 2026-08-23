package at.hannibal2.skyhanni.config.commands.brigadier

import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils.dynamicSuggestionProvider
import at.hannibal2.skyhanni.features.commands.tabcomplete.PlayerNameSource
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource

/**
 * Builder-backed utility for creating dynamic Brigadier player suggestions.
 *
 * Supports combining player categories and explicit names, then filtering or
 * excluding entries before converting them into a [SuggestionProvider].
 */
class PlayerSuggestions private constructor(
    private val sequence: Sequence<String>,
) {

    fun getPlayers(): List<String> =
        getSequence().toList()

    fun getSequence(): Sequence<String> = sequence.distinct()

    fun toSuggestionProvider(): SuggestionProvider<FabricClientCommandSource> {
        return dynamicSuggestionProvider { getPlayers() }
    }

    class Builder {
        private var seq: Sequence<String> = emptySequence()

        fun includeAllSources(): Builder {
            seq += PlayerNameSource.entries.asSequence().flatMap { it.usernames }
            return this
        }

        fun include(vararg categories: PlayerNameSource): Builder {
            seq += categories.asSequence().flatMap { it.usernames }
            return this
        }

        fun include(categories: Collection<PlayerNameSource>): Builder {
            seq += categories.asSequence().flatMap { it.usernames }
            return this
        }

        fun exclude(vararg categories: PlayerNameSource): Builder {
            val oldSeq = seq
            seq = sequence {
                val excluded = categories
                    .asSequence()
                    .flatMap { it.usernames }
                    .toSet()

                yieldAll(oldSeq.filterNot { it in excluded })
            }
            return this
        }

        fun includePlayers(vararg players: String): Builder {
            seq += players.asSequence()
            return this
        }

        fun includePlayers(players: Collection<String>): Builder {
            seq += players.asSequence()
            return this
        }

        fun excludePlayers(vararg players: String): Builder {
            val excluded = players.toSet()
            seq = seq.filterNot { it in excluded }
            return this
        }

        fun filter(predicate: (String) -> Boolean): Builder {
            seq = seq.filter(predicate)
            return this
        }

        fun filterNot(predicate: (String) -> Boolean): Builder {
            seq = seq.filterNot(predicate)
            return this
        }

        fun build(): PlayerSuggestions = PlayerSuggestions(seq)
    }

    companion object {
        fun builder(block: Builder.() -> Unit): SuggestionProvider<FabricClientCommandSource> {
            return buildPlayerSuggestions(block).toSuggestionProvider()
        }

        fun buildPlayerSuggestions(block: Builder.() -> Unit): PlayerSuggestions {
            return Builder()
                .apply(block)
                .build()
        }
    }
}
