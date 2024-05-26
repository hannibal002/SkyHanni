package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.features.misc.MarkedPlayerManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PlayerDeathMessages {

    private val lastTimePlayerSeen = mutableMapOf<String, Long>()

    //§c ☠ §r§7§r§bZeroHazel§r§7 was killed by §r§8§lAshfang§r§7§r§7.
    private val deathMessagePattern by RepoPattern.pattern(
        "chat.player.death",
        "§c ☠ §r§7§r§.(?<name>.+)§r§7 (?<reason>.+)"
    )

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isHideFarDeathsEnabled()) return

        checkOtherPlayers()
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val message = event.message
        deathMessagePattern.matchMatcher(message) {
            val name = group("name")
            if (MarkedPlayerManager.config.highlightInChat &&
                !DungeonAPI.inDungeon() && !LorenzUtils.inKuudraFight && MarkedPlayerManager.isMarkedPlayer(name)
            ) {
                val reason = group("reason").removeColor()

                val color = MarkedPlayerManager.config.chatColor.getChatColor()
                ChatUtils.chat(" §c☠ $color$name §7$reason", false)
                event.blockedReason = "marked_player_death"
                return
            }

            val time = System.currentTimeMillis() > lastTimePlayerSeen.getOrDefault(name, 0) + 30_000
            if (isHideFarDeathsEnabled() && time) {
                event.blockedReason = "far_away_player_death"
            }
        }
    }

    private fun checkOtherPlayers() {
        val location = LocationUtils.playerLocation()
        for (otherPlayer in EntityUtils.getEntities<EntityOtherPlayerMP>()
            .filter { it.getLorenzVec().distance(location) < 25 }) {
            lastTimePlayerSeen[otherPlayer.name] = System.currentTimeMillis()
        }
    }

    private fun isHideFarDeathsEnabled(): Boolean {
        return LorenzUtils.inSkyBlock && SkyHanniMod.feature.chat.hideFarDeathMessages && !DungeonAPI.inDungeon() && !LorenzUtils.inKuudraFight
    }
}
