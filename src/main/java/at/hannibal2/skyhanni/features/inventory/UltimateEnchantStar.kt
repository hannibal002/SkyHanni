package at.hannibal2.hanni.features.inventory

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.RenderItemTipEvent
import at.hannibal2.hanni.events.RenderObject
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.SkyBlockItemModifierUtils.getHypixelEnchantments
import at.hannibal2.hanni.utils.SkyBlockUtils
import net.minecraft.init.Items

@HanniModule
object UltimateEnchantStar {

    private val config get() = HanniMod.feature.inventory

    @HandleEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        if (!isEnabled()) return
        if (event.stack.item != Items.enchanted_book) return
        val enchants = event.stack.getHypixelEnchantments() ?: return
        if (enchants.size != 1 || !enchants.keys.first().startsWith("ultimate_")) return
        event.renderObjects += RenderObject("§d✦", -10, -10)
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.ultimateEnchantStar

}
