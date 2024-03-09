package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.enums.OutsideSbFeature
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.withAlpha
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MarkedPlayerManager {
    companion object {

        private val config get() = SkyHanniMod.feature.markedPlayers

        val playerNamesToMark = mutableListOf<String>()
        private val markedPlayers = mutableMapOf<String, EntityOtherPlayerMP>()

        fun command(args: Array<String>) {
            if (args.size != 1) {
                ChatUtils.userError("Usage: /shmarkplayer <name>")
                return
            }

            val displayName = args[0]
            val name = displayName.lowercase()


            if (name == LorenzUtils.getPlayerName().lowercase()) {
                ChatUtils.userError("You can't add or remove yourself this way! Go to the settings and toggle 'Mark your own name'.")
                return
            }

            if (name !in playerNamesToMark) {
                playerNamesToMark.add(name)
                findPlayers()
                ChatUtils.chat("§aMarked §eplayer §b$displayName§e!")
            } else {
                playerNamesToMark.remove(name)
                markedPlayers[name]?.let { RenderLivingEntityHelper.removeCustomRender(it) }
                markedPlayers.remove(name)
                ChatUtils.chat("§cUnmarked §eplayer §b$displayName§e!")
            }
        }

        private fun findPlayers() {
            for (entity in EntityUtils.getEntities<EntityOtherPlayerMP>()) {
                if (entity in markedPlayers.values) continue

                val name = entity.name.lowercase()
                if (name in playerNamesToMark) {
                    markedPlayers[name] = entity
                    RenderLivingEntityHelper.setEntityColorWithNoHurtTime(
                        entity,
                        config.entityColor.toColor().withAlpha(127),
                        ::isEnabled
                    )
                }
            }
        }

        fun isMarkedPlayer(player: String): Boolean = player.lowercase() in playerNamesToMark

        private fun isEnabled() = (LorenzUtils.inSkyBlock || OutsideSbFeature.MARKED_PLAYERS.isSelected())
            && config.highlightInWorld
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        config.markOwnName.whenChanged { _, new ->
            val name = LorenzUtils.getPlayerName()
            if (new) {
                if (!playerNamesToMark.contains(name)) {
                    playerNamesToMark.add(name)
                }
            } else {
                playerNamesToMark.remove(name)
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return

        if (event.repeatSeconds(1)) {
            findPlayers()
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        if (Minecraft.getMinecraft().thePlayer == null) return

        markedPlayers.clear()
        if (config.markOwnName.get()) {
            val name = LorenzUtils.getPlayerName()
            if (!playerNamesToMark.contains(name)) {
                playerNamesToMark.add(name)
            }
        }
    }
}
