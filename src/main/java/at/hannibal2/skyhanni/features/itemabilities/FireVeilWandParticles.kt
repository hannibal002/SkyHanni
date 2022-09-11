package at.hannibal2.skyhanni.features.itemabilities

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ItemClickInHandEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SpecialColour
import net.minecraft.client.Minecraft
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color

class FireVeilWandParticles {

    var lastClick = 0L

    @SubscribeEvent
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        if (!LorenzUtils.inSkyblock) return
        if (SkyHanniMod.feature.itemAbilities.fireVeilWandDisplay == 0) return
        if (System.currentTimeMillis() > lastClick + 5_500) return

        val packet = event.packet
        if (packet !is S2APacketParticles) return


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
        if (!LorenzUtils.inSkyblock) return
        if (event.clickType != ItemClickInHandEvent.ClickType.RIGHT_CLICK) return

        val itemInHand = event.itemInHand ?: return

        val internalName = itemInHand.getInternalName()
        if (internalName == "FIRE_VEIL_WAND") {
            lastClick = System.currentTimeMillis()
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!LorenzUtils.inSkyblock) return
        if (SkyHanniMod.feature.itemAbilities.fireVeilWandDisplay != 1) return
        if (System.currentTimeMillis() > lastClick + 5_500) return

        val color =
            Color(SpecialColour.specialToChromaRGB(SkyHanniMod.feature.itemAbilities.fireVeilWandDisplayColor), true)

        RenderUtils.drawCircle(Minecraft.getMinecraft().thePlayer, event.partialTicks, 3.5, color)
    }
}