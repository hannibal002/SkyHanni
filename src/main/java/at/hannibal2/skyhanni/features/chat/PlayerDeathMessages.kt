package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.misc.MarkedPlayerManager
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PlayerDeathMessages {

    private val lastTimePlayerSeen = mutableMapOf<String, Long>()

    // TODO USE SH-REPO
    //§c ☠ §r§7§r§bZeroHazel§r§7 was killed by §r§8§lAshfang§r§7§r§7.
    private val deathMessagePattern = "§c ☠ §r§7§r§.(?<name>.+)§r§7 (?<reason>.+)".toPattern()

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isHideFarDeathsEnabled()) return

        if (event.repeatSeconds(1)) {
            checkOtherPlayers()
        }
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val message = event.message
        deathMessagePattern.matchMatcher(message) {
            val name = group("name")
            if (SkyHanniMod.feature.markedPlayers.highlightInChat && !LorenzUtils.inDungeons && !LorenzUtils.inKuudraFight && MarkedPlayerManager.isMarkedPlayer(
                    name
                )
            ) {
                val reason = group("reason").removeColor()
                LorenzUtils.chat(" §c☠ §e$name §7$reason", false)
                event.blockedReason = "marked_player_death"
                return
            }


            if (isHideFarDeathsEnabled() && System.currentTimeMillis() > lastTimePlayerSeen.getOrDefault(
                    name,
                    0
                ) + 30_000
            ) {
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
        return LorenzUtils.inSkyBlock && SkyHanniMod.feature.chat.hideFarDeathMessages && !LorenzUtils.inDungeons && !LorenzUtils.inKuudraFight
    }
}
