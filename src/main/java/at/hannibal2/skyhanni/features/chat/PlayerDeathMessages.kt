package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.player.PlayerDeathEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.features.misc.MarkedPlayerManager
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.client.entity.EntityOtherPlayerMP
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
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
        SkyBlockUtils.inSkyBlock && SkyHanniMod.feature.chat.hideFarDeathMessages && !DungeonApi.inDungeon() && !KuudraApi.inKuudra
}
