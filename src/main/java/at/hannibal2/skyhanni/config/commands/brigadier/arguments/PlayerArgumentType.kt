package at.hannibal2.skyhanni.config.commands.brigadier.arguments

import at.hannibal2.skyhanni.config.commands.brigadier.PlayerSource
import at.hannibal2.skyhanni.config.commands.brigadier.PlayerSuggestions
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import java.util.concurrent.CompletableFuture

class PlayerArgumentType private constructor(val suggestions: PlayerSuggestions) : ArgumentType<Player> {
    override fun parse(reader: StringReader): Player {
        val username = if (reader.canRead() && reader.peek() == '"') {
            reader.readQuotedString()
        } else {
            reader.readUnquotedString()
        }

        return getAllPlayers()
            .firstOrNull { it.gameProfile.name.equals(username, ignoreCase = true) }
            ?: throw PLAYER_NOT_FOUND.create(username)
    }

    override fun <S> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        return suggestions.listSuggestions(builder)
    }

    override fun getExamples(): Collection<String> =
        listOf("Notch", "Technoblade", "hannibal02")

    companion object {
        private val PLAYER_NOT_FOUND = DynamicCommandExceptionType {
            Component.literal("Could not find player with username: $it")
        }

        private fun getAllPlayers(): List<Player> {
            return EntityUtils.getPlayerEntities() + listOfNotNull(MinecraftCompat.localPlayerOrNull)
        }

        fun player(
            include: Set<PlayerSource> = setOf(PlayerSource.WORLD),
            exclude: Set<PlayerSource> = emptySet(),
        ): PlayerArgumentType {
            return PlayerArgumentType(PlayerSuggestions.create(include, exclude))
        }
    }
}
