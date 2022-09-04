package at.hannibal2.skyhanni.features

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.RenderMobColoredEvent
import at.hannibal2.skyhanni.events.ResetEntityHurtTimeEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.server.S1CPacketEntityMetadata
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CorruptedMobHighlight {

    private val corruptedMobs = mutableListOf<EntityLivingBase>()

    @SubscribeEvent
    fun onHealthUpdatePacket(event: PacketEvent.ReceiveEvent) {
        val packet = event.packet
        if (packet !is S1CPacketEntityMetadata) return

        for (watchableObject in packet.func_149376_c()) {
            if (watchableObject.dataValueId != 6) continue

            val entity = Minecraft.getMinecraft().theWorld.getEntityByID(packet.entityId) ?: continue
            if (entity !is EntityLivingBase) continue

            val health = watchableObject.`object` as Float
            val baseMaxHealth = entity.baseMaxHealth.toFloat()
            if (health == baseMaxHealth * 3) {
                corruptedMobs.add(entity)
            }
        }
    }

    @SubscribeEvent
    fun onRenderMobColored(event: RenderMobColoredEvent) {
        if (!isEnabled()) return
        val entity = event.entity

        if (entity in corruptedMobs) {
            event.color = LorenzColor.DARK_PURPLE.toColor().withAlpha(127)
        }
    }

    @SubscribeEvent
    fun onResetEntityHurtTime(event: ResetEntityHurtTimeEvent) {
        if (!isEnabled()) return
        val entity = event.entity

        if (entity in corruptedMobs) {
            event.shouldReset = true
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        corruptedMobs.clear()
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyblock && SkyHanniMod.feature.misc.corruptedMobHighlight &&
                LorenzUtils.skyBlockIsland != "Private Island"
    }
}