package at.hannibal2.skyhanni.features.itemabilities

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.toChromaColor
import at.hannibal2.skyhanni.utils.RenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class FireVeilWandParticles {

    var lastClick = 0L

    @SubscribeEvent
    fun onChatPacket(event: ReceiveParticleEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (SkyHanniMod.feature.itemAbilities.fireVeilWandDisplay == 0) return
        if (System.currentTimeMillis() > lastClick + 5_500) return

        if (event.type == EnumParticleTypes.FLAME && event.count == 1 && event.speed == 0f && event.offset.isZero()) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onBlockClick(event: BlockClickEvent) {
        if (!LorenzUtils.inSkyBlock) return

        if (event.clickType == ClickType.RIGHT_CLICK) {
            val internalName = event.itemInHand?.getInternalName() ?: return

            if (internalName.equals("FIRE_VEIL_WAND")) {
                lastClick = System.currentTimeMillis()
            }
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (SkyHanniMod.feature.itemAbilities.fireVeilWandDisplay != 1) return
        if (System.currentTimeMillis() > lastClick + 5_500) return

        val color = SkyHanniMod.feature.itemAbilities.fireVeilWandDisplayColor.toChromaColor()

        RenderUtils.drawCircle(Minecraft.getMinecraft().thePlayer, event.partialTicks, 3.5, color)
    }
}