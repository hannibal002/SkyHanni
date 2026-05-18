package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.player.PlayerDeathEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.features.misc.MarkedPlayerManager
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import net.minecraft.ChatFormatting
import net.minecraft.client.player.RemotePlayer
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PlayerDeathMessages {

    private val lastTimePlayerSeen = mutableMapOf<String, SimpleTimeMark>()

    @HandleEvent
    fun onSecondPassed() {
        if (!shouldHideFarDeaths()) return

        EntityUtils.getEntitiesNearby<RemotePlayer>(25.0).forEach { player ->
            lastTimePlayerSeen[player.cleanName()] = SimpleTimeMark.now()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onAllowPlayerDeath(event: PlayerDeathEvent.Allow) {
        if (event.isSelf) return

        val lastTime = lastTimePlayerSeen[event.name] ?: SimpleTimeMark.farPast()
        val time = lastTime.passedSince() > 30.seconds

        if (shouldHideFarDeaths() && time) {
            event.chatEvent.blockedReason = "far_away_player_death"
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onModifyPlayerDeath(event: PlayerDeathEvent.Modify) {
        if (event.isSelf || DungeonApi.inDungeon() || KuudraApi.inKuudra) return

        val name = event.name

        if (MarkedPlayerManager.config.highlightInChat && MarkedPlayerManager.isMarkedPlayer(name)) {
            val reason = event.reason
            val color = MarkedPlayerManager.config.chatColor.toChatFormatting()
            event.chatEvent.replaceComponent(
                componentBuilder {
                    appendWithColor(" ☠ ", ChatFormatting.RED)
                    appendWithColor("$name ", color)
                    appendWithColor(reason, ChatFormatting.GRAY)
                },
                "marked_player_death",
            )
        }
    }

    private fun shouldHideFarDeaths(): Boolean =
        SkyBlockUtils.inSkyBlock &&
            SkyHanniMod.feature.chat.hideFarDeathMessages &&
            !DungeonApi.inDungeon() &&
            !KuudraApi.inKuudra
}
