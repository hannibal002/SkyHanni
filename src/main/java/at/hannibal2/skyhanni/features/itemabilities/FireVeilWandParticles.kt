package at.hannibal2.skyhanni.features.itemabilities

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.toChromaColor
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.RenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class FireVeilWandParticles {
    private val config get() = SkyHanniMod.feature.itemAbilities.fireVeilWands

    private var lastClick = 0L

    val item by lazy { "FIRE_VEIL_WAND".asInternalName() }

    @SubscribeEvent
    fun onChatPacket(event: ReceiveParticleEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (config.display == 0) return
        if (System.currentTimeMillis() > lastClick + 5_500) return
        if (event.type == EnumParticleTypes.FLAME && event.speed == 0.55f) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onBlockClick(event: BlockClickEvent) {
        if (!LorenzUtils.inSkyBlock) return

        if (event.clickType == ClickType.RIGHT_CLICK) {
            val internalName = event.itemInHand?.getInternalName() ?: return

            if (internalName == item) {
                lastClick = System.currentTimeMillis()
            }
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (config.display != 1) return
        if (System.currentTimeMillis() > lastClick + 5_500) return

        val color = config.displayColor.toChromaColor()

        RenderUtils.drawCircle(Minecraft.getMinecraft().thePlayer, event.partialTicks, 3.5, color)
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "itemAbilities.fireVeilWandDisplayColor", "itemAbilities.fireVeilWands.displayColor")
        event.move(3, "itemAbilities.fireVeilWandDisplay", "itemAbilities.fireVeilWands.display")
    }
}
