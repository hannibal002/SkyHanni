package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.RenderMobColoredEvent
import at.hannibal2.skyhanni.events.ResetEntityHurtEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class MarkedPlayerManager {

    companion object {
        val playerNamesToMark = mutableListOf<String>()
        private val markedPlayers = mutableMapOf<String, EntityOtherPlayerMP>()

        fun command(args: Array<String>) {
            if (args.size != 1) {
                LorenzUtils.chat("§cUsage: /shmarkplayer <name>")
                return
            }

            val displayName = args[0]
            val name = displayName.lowercase()


            if (name == LorenzUtils.getPlayerName().lowercase()) {
                LorenzUtils.chat("§c[SkyHanni] You can't add or remove yourself this way! Go to the settings and toggle 'Mark your own name'.")
                return
            }

            if (name !in playerNamesToMark) {
                playerNamesToMark.add(name)
                findPlayers()
                LorenzUtils.chat("§e[SkyHanni] §aMarked §eplayer §b$displayName§e!")
            } else {
                playerNamesToMark.remove(name)
                markedPlayers.remove(name)
                LorenzUtils.chat("§e[SkyHanni] §cUnmarked §eplayer §b$displayName§e!")
            }
        }

        private fun findPlayers() {
            for (entity in Minecraft.getMinecraft().theWorld.loadedEntityList) {
                if (entity is EntityOtherPlayerMP) {
                    if (entity in markedPlayers.values) continue

                    val name = entity.name.lowercase()
                    if (name in playerNamesToMark) {
                        markedPlayers[name] = entity
                    }
                }
            }
        }

        fun isMarkedPlayer(player: String): Boolean = player.lowercase() in playerNamesToMark

    }


    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        SkyHanniMod.feature.markedPlayers.markOwnName.whenChanged { _, new ->
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

    var tick = 0

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!LorenzUtils.inSkyBlock) return

        if (tick++ % 20 == 0) {
            findPlayers()
        }
    }

    @SubscribeEvent
    fun onRenderMobColored(event: RenderMobColoredEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.markedPlayers.highlightInWorld) return

        val entity = event.entity
        if (entity in markedPlayers.values) {
            event.color = LorenzColor.YELLOW.toColor().withAlpha(127)
        }
    }

    @SubscribeEvent
    fun onResetEntityHurtTime(event: ResetEntityHurtEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.markedPlayers.highlightInWorld) return

        val entity = event.entity
        if (entity in markedPlayers.values) {
            event.shouldReset = true
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        if (Minecraft.getMinecraft().thePlayer == null) return

        markedPlayers.clear()
        if (SkyHanniMod.feature.markedPlayers.markOwnName.get()) {
            val name = LorenzUtils.getPlayerName()
            if (!playerNamesToMark.contains(name)) {
                playerNamesToMark.add(name)
            }
        }
    }
}