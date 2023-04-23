package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.misc.MarkedPlayerManager
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class PlayerDeathMessages {

    private var tick = 0
    private val lastTimePlayerSeen = mutableMapOf<String, Long>()

    //§c ☠ §r§7§r§bZeroHazel§r§7 was killed by §r§8§lAshfang§r§7§r§7.
    private val pattern = "§c ☠ §r§7§r§.(.+)§r§7 (.+)".toPattern()

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isHideFarDeathsEnabled()) return

        if (tick++ % 20 == 0) {
            checkOtherPlayers()
        }
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val message = event.message
        val matcher = pattern.matcher(message)
        if (matcher.matches()) {
            val name = matcher.group(1)
            if (SkyHanniMod.feature.markedPlayers.highlightInChat && !LorenzUtils.inDungeons && !LorenzUtils.inKuudraFight) {
                if (MarkedPlayerManager.isMarkedPlayer(name)) {
                    val reason = matcher.group(2).removeColor()
                    LorenzUtils.chat(" §c☠ §e$name §7$reason")
                    event.blockedReason = "marked_player_death"
                    return
                }
            }


            if (isHideFarDeathsEnabled()) {
                if (System.currentTimeMillis() > lastTimePlayerSeen.getOrDefault(name, 0) + 30_000) {
                    event.blockedReason = "far_away_player_death"
                }
            }
        }
    }

    private fun checkOtherPlayers() {
        val location = LocationUtils.playerLocation()
        for (otherPlayer in Minecraft.getMinecraft().theWorld.loadedEntityList
            .filterIsInstance<EntityOtherPlayerMP>()
            .filter { it.getLorenzVec().distance(location) < 25 }) {
            lastTimePlayerSeen[otherPlayer.name] = System.currentTimeMillis()
        }
    }

    private fun isHideFarDeathsEnabled(): Boolean {
        return LorenzUtils.inSkyBlock && SkyHanniMod.feature.chat.hideFarDeathMessages && !LorenzUtils.inDungeons && !LorenzUtils.inKuudraFight
    }
}