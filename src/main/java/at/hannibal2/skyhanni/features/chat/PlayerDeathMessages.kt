package at.hannibal2.hanni.features.chat

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.events.player.PlayerDeathEvent
import at.hannibal2.hanni.features.dungeon.DungeonApi
import at.hannibal2.hanni.features.misc.MarkedPlayerManager
import at.hannibal2.hanni.features.nether.kuudra.KuudraApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.EntityUtils
import at.hannibal2.hanni.utils.LocationUtils
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.getLorenzVec
import net.minecraft.client.entity.EntityOtherPlayerMP
import kotlin.time.Duration.Companion.seconds

@HanniModule
object PlayerDeathMessages {

    private val lastTimePlayerSeen = mutableMapOf<String, SimpleTimeMark>()

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isHideFarDeathsEnabled()) return

        checkOtherPlayers()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onPlayerDeath(event: PlayerDeathEvent) {

        val name = event.name

        if (MarkedPlayerManager.config.highlightInChat &&
            !DungeonApi.inDungeon() &&
            !KuudraApi.inKuudra &&
            MarkedPlayerManager.isMarkedPlayer(name)
        ) {
            val reason = event.reason
            val color = MarkedPlayerManager.config.chatColor.getChatColor()
            ChatUtils.chat(" §c☠ $color$name §7$reason", false)
            event.chatEvent.blockedReason = "marked_player_death"
            return
        }

        val lastTime = lastTimePlayerSeen[name] ?: SimpleTimeMark.farPast()
        val time = lastTime.passedSince() > 30.seconds

        if (isHideFarDeathsEnabled() && time) {
            event.chatEvent.blockedReason = "far_away_player_death"
        }
    }

    private fun checkOtherPlayers() {
        val entities = EntityUtils.getEntities<EntityOtherPlayerMP>()
            .filter { it.getLorenzVec().distance(LocationUtils.playerLocation()) < 25 }
        for (otherPlayer in entities) {
            lastTimePlayerSeen[otherPlayer.name] = SimpleTimeMark.now()
        }
    }

    private fun isHideFarDeathsEnabled(): Boolean =
        SkyBlockUtils.inSkyBlock && HanniMod.feature.chat.hideFarDeathMessages && !DungeonApi.inDungeon() && !KuudraApi.inKuudra
}
