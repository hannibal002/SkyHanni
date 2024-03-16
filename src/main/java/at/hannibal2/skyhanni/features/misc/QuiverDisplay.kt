package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.QuiverAPI
import at.hannibal2.skyhanni.data.QuiverAPI.NONE_ARROW_TYPE
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class QuiverDisplay {

    private val config get() = SkyHanniMod.feature.misc.quiverDisplay
    private var display = emptyList<Renderable>()

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        display = emptyList()
    }

    private fun updateDisplay() {
        display = drawDisplay()
    }

    private fun drawDisplay() = buildList {
        val arrowType = QuiverAPI.currentArrow ?: return@buildList
        val arrowAmount = QuiverAPI.currentAmount
        val itemStack = NEUItems.getItemStackOrNull(arrowType.internalName.asString()) ?: ItemStack(Items.arrow)

        val rarity = itemStack.getItemRarityOrNull()?.chatColorCode ?: "§f"
        val arrowName = if (arrowType != NONE_ARROW_TYPE) StringUtils.pluralize(arrowAmount, arrowType.arrow) else arrowType.arrow

        if (config.showIcon) {
            add(Renderable.itemStack(itemStack,1.68))
        }

        add(Renderable.string("§b${arrowAmount}x $rarity$arrowName"))
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        updateDisplay()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (display.isEmpty()) return
        config.quiverDisplayPos.renderStringsAndItems(listOf(display), posLabel = "Quiver Display")
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
