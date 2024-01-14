package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderItemEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.drawSlotText
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPetItem
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PetItemDisplay {

    private val configList get() = SkyHanniMod.feature.misc.pets.petItemDisplay

    @SubscribeEvent
    fun onRenderItemOverlayPost(event: GuiRenderItemEvent.RenderOverlayEvent.GuiRenderItemPost) {
        val stack = event.stack ?: return
        if (!LorenzUtils.inSkyBlock || stack.stackSize != 1) return
        if (configList.isEmpty()) return

        val petItem = stack.getPetItem() ?: return
        val icon = configList.firstOrNull { it.item == petItem }?.icon ?: return

        val x = event.x + 17
        val y = event.y - 1

        event.drawSlotText(x, y, icon, .9f)
    }
}
