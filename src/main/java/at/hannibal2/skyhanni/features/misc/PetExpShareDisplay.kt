package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderItemEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.drawSlotText
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPetItem
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PetExpShareDisplay {

    @SubscribeEvent
    fun onRenderItemOverlayPost(event: GuiRenderItemEvent.RenderOverlayEvent.GuiRenderItemPost) {
        val stack = event.stack ?: return
        if (!LorenzUtils.inSkyBlock || stack.stackSize != 1) return
        if (!SkyHanniMod.feature.misc.pets.expShare) return

        val petItem = stack.getPetItem() ?: return
        if (petItem != "PET_ITEM_EXP_SHARE") return

        val stackTip = "§5⚘"
        val x = event.x + 17
        val y = event.y - 1

        event.drawSlotText(x, y, stackTip, .9f)
    }
}
