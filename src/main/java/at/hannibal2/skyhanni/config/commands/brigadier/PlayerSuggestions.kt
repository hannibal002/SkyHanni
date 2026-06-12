package at.hannibal2.skyhanni.config.commands.brigadier

import at.hannibal2.skyhanni.data.GuildApi
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.features.misc.CarryTracker
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import java.util.concurrent.CompletableFuture

class PlayerSuggestions private constructor(
    private val include: Set<PlayerSource>,
    private val exclude: Set<PlayerSource>,
) : SuggestionProvider<FabricClientCommandSource> {

    fun usernames(): Set<String> {
        val included = include.flatMapTo(linkedSetOf()) { it.usernames() }
        val excluded = exclude.flatMapTo(linkedSetOf()) { it.usernames() }

        return included - excluded
    }

    fun listSuggestions(
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        val remaining = builder.remainingLowerCase

        usernames()
            .asSequence()
            .filter { it.lowercase().startsWith(remaining) }
            .forEach(builder::suggest)

        return builder.buildFuture()
    }

    override fun getSuggestions(
        context: CommandContext<FabricClientCommandSource>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        return listSuggestions(builder)
    }

    companion object {
        fun create(
            include: Set<PlayerSource> = setOf(PlayerSource.WORLD),
            exclude: Set<PlayerSource> = emptySet(),
        ): PlayerSuggestions {
            return PlayerSuggestions(include, exclude)
        }
    }
}

enum class PlayerSource {
    WORLD,
    SELF,
    PARTY,
    GUILD,
    CARRY_CUSTOMER,
    ;

    fun usernames(): Set<String> = when (this) {
        WORLD ->
            EntityUtils.getPlayerEntities()
                .mapTo(linkedSetOf()) { it.gameProfile.name }
        SELF ->
            setOfNotNull(
                MinecraftCompat.localPlayerOrNull?.gameProfile?.name,
            )
        PARTY -> PartyApi.partyMembers.toSet()
        GUILD -> GuildApi.getAllMembers().toSet()
        CARRY_CUSTOMER -> CarryTracker.customers.map { it.name }.toSet()
    }
}
