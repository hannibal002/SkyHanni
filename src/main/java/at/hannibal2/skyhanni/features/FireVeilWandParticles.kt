package at.hannibal2.skyhanni.features

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ItemClickInHandEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class FireVeilWandParticles {

    var lastClick = 0L

    @SubscribeEvent
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        if (!isEnabled()) return

        val packet = event.packet
        if (packet !is S2APacketParticles) return

        if (System.currentTimeMillis() > lastClick + 5_500) return

        if (packet.particleType == EnumParticleTypes.FLAME && packet.particleCount == 1 && packet.particleSpeed == 0f &&
            packet.xOffset == 0f &&
            packet.yOffset == 0f &&
            packet.zOffset == 0f
        ) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onItemClick(event: ItemClickInHandEvent) {
        if (!isEnabled()) return
        if (event.clickType != ItemClickInHandEvent.ClickType.RIGHT_CLICK) return

        val itemInHand = event.itemInHand ?: return

        val internalName = itemInHand.getInternalName()
        if (internalName == "FIRE_VEIL_WAND") {
            lastClick = System.currentTimeMillis()
        }
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyblock && SkyHanniMod.feature.abilities.fireVeilWandHider
    }
}