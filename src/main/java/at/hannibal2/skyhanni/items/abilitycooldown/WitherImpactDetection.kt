package at.hannibal2.skyhanni.items.abilitycooldown

import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.ItemUtil.asStringSet
import at.hannibal2.skyhanni.utils.ItemUtil.getExtraAttributes
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraft.init.Items
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.server.S1CPacketEntityMetadata
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.common.util.Constants
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

/**
 * Taken from Skytils under AGPL 3.0
 * Modified
 * https://github.com/Skytils/SkytilsMod/blob/1.x/LICENSE.md
 * @author Skytils
 */
class WitherImpactDetection(private val itemAbilityCooldown: ItemAbilityCooldown) {

    val S2APacketParticles.type: EnumParticleTypes
        get() = this.particleType
    var lastShieldUse = -1L
    var lastShieldClick = 0L

    @SubscribeEvent
    fun onReceivePacket(event: PacketEvent.ReceiveEvent) {
        val mc = Minecraft.getMinecraft()
        if (!LorenzUtils.inSkyblock || mc.theWorld == null) return

        event.packet.apply {

            if (this is S1CPacketEntityMetadata && lastShieldClick != -1L && entityId == mc.thePlayer?.entityId && System.currentTimeMillis() - lastShieldClick <= 500 && func_149376_c()?.any { it.dataValueId == 17 } == true) {
                lastShieldUse = System.currentTimeMillis()
                lastShieldClick = -1
                itemAbilityCooldown.clickWitherImpact()
            }
        }
    }

    @SubscribeEvent
    fun onSendPacket(event: PacketEvent.SendEvent) {
        val mc = Minecraft.getMinecraft()
        if (!LorenzUtils.inSkyblock || lastShieldUse != -1L || mc.thePlayer?.heldItem == null) return
        if (event.packet is C08PacketPlayerBlockPlacement && mc.thePlayer.heldItem.item == Items.iron_sword && getExtraAttributes(
                mc.thePlayer.heldItem
            )?.getTagList("ability_scroll", Constants.NBT.TAG_STRING)?.asStringSet()
                ?.contains("WITHER_SHIELD_SCROLL") == true
        ) {
            lastShieldClick = System.currentTimeMillis()
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (lastShieldUse != -1L) {
            val diff = ((lastShieldUse + 5000 - System.currentTimeMillis()) / 1000f)
            if (diff < 0) lastShieldUse = -1
        }
    }
}