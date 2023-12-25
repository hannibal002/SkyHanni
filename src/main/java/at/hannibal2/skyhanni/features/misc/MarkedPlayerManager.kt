package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.enums.OutsideSbFeature
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.RenderMobColoredEvent
import at.hannibal2.skyhanni.events.ResetEntityHurtEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MarkedPlayerManager {
    private val config get() = SkyHanniMod.feature.markedPlayers

    companion object {
        val playerNamesToMark = mutableListOf<String>()
        private val markedPlayers = mutableMapOf<String, EntityOtherPlayerMP>()

        fun command(args: Array<String>) {
            if (args.size != 1) {
                LorenzUtils.userError("Usage: /shmarkplayer <name>")
                return
            }

            val displayName = args[0]
            val name = displayName.lowercase()


            if (name == LorenzUtils.getPlayerName().lowercase()) {
                LorenzUtils.userError("You can't add or remove yourself this way! Go to the settings and toggle 'Mark your own name'.")
                return
            }

            if (name !in playerNamesToMark) {
                playerNamesToMark.add(name)
                findPlayers()
                LorenzUtils.chat("§aMarked §eplayer §b$displayName§e!")
            } else {
                playerNamesToMark.remove(name)
                markedPlayers.remove(name)
                LorenzUtils.chat("§cUnmarked §eplayer §b$displayName§e!")
            }
        }

        private fun findPlayers() {
            for (entity in EntityUtils.getEntities<EntityOtherPlayerMP>()) {
                if (entity in markedPlayers.values) continue

                val name = entity.name.lowercase()
                if (name in playerNamesToMark) {
                    markedPlayers[name] = entity
                }
            }
        }

        fun isMarkedPlayer(player: String): Boolean = player.lowercase() in playerNamesToMark

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
    fun onRenderMobColored(event: RenderMobColoredEvent) {
        if (!isEnabled()) return

        val entity = event.entity
        if (entity in markedPlayers.values) {
            event.color = config.entityColor.toColor().withAlpha(127)
        }
    }

    @SubscribeEvent
    fun onResetEntityHurtTime(event: ResetEntityHurtEvent) {
        if (!isEnabled()) return

        val entity = event.entity
        if (entity in markedPlayers.values) {
            event.shouldReset = true
        }
    }

    private fun isEnabled() = config.highlightInWorld && (LorenzUtils.inSkyBlock || OutsideSbFeature.MARKED_PLAYERS.isSelected())

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
