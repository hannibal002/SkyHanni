package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderItemEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.drawSlotText
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPetCandyUsed
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PetCandyUsedDisplay {

    @SubscribeEvent
    fun onRenderItemOverlayPost(event: GuiRenderItemEvent.RenderOverlayEvent.GuiRenderItemPost) {
        val stack = event.stack ?: return
        if (!LorenzUtils.inSkyBlock || stack.stackSize != 1) return
        if (!SkyHanniMod.feature.misc.petCandyUsed) return


        val petCandyUsed = stack.getPetCandyUsed() ?: return
        if (petCandyUsed == 0) return

        val stackTip = "§c$petCandyUsed"
        val x = event.x + 13
        val y = event.y + 1

        event.drawSlotText(x, y, stackTip, .9f)
    }
}