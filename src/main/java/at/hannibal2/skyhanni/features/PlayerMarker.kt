package at.hannibal2.skyhanni.features

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

class PlayerMarker {

    companion object {
        private val playerNamesToMark = mutableListOf<String>()
        private val markedPlayers = mutableMapOf<String, EntityOtherPlayerMP>()

        fun command(args: Array<String>) {
            if (args.size != 1) {
                LorenzUtils.chat("§cUsage: /shmarkplayer <name>")
                return
            }

            val displayName = args[0]
            val name = displayName.lowercase()
            if (name !in playerNamesToMark) {
                playerNamesToMark.add(name)
                findPlayers()
                LorenzUtils.chat("§e[SkyHanni] §amarked §eplayer §b$displayName!")
            } else {
                playerNamesToMark.remove(name)
                markedPlayers.remove(name)
                LorenzUtils.chat("§e[SkyHanni] §cunmarked §eplayer §b$displayName!")
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
    }

    var tick = 0

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return

        if (tick++ % 20 == 0) {
            findPlayers()
        }
    }

    @SubscribeEvent
    fun onRenderMobColored(event: RenderMobColoredEvent) {
        if (!isEnabled()) return
        val entity = event.entity
        if (entity in markedPlayers.values) {
            event.color = LorenzColor.YELLOW.toColor().withAlpha(127)
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

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        markedPlayers.clear()
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyblock
    }
}