package at.hannibal2.skyhanni.config.commands.brigadier.arguments

import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player

class PlayerArgumentType private constructor() : ArgumentType<Player> {
    override fun parse(reader: StringReader): Player {
        val username = reader.readString()
        return getAllPlayers()
            .firstOrNull { it.name.string.equals(username, ignoreCase = true) }
            ?: throw PLAYER_NOT_FOUND.create(username)
    }

    private fun getAllPlayers(): List<Player> =
        EntityUtils.getPlayerEntities() + listOfNotNull(MinecraftCompat.localPlayerOrNull)

    override fun getExamples(): Collection<String> =
        listOf("Notch", "Technoblade", "hannibal02")

    companion object {
        private val PLAYER_NOT_FOUND = DynamicCommandExceptionType {
            Component.literal("Could not find player with username: $it")
        }

        fun player(): PlayerArgumentType {
            return PlayerArgumentType()
        }
    }
}
