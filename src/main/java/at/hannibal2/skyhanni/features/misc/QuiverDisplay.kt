package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.QuiverAPI
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.QuiverUpdateEvent
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
    private var arrow = QuiverAPI.currentArrow
    private var amount = 0
    private var hideAmount = false

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        display = emptyList()
        updateDisplay()
    }

    private fun updateDisplay() {
        display = drawDisplay()
    }

    private fun drawDisplay() = buildList {
        val arrow = arrow ?: return@buildList
        val itemStack = NEUItems.getItemStackOrNull(arrow.internalName.asString()) ?: ItemStack(Items.arrow)

        val rarity = itemStack.getItemRarityOrNull()?.chatColorCode ?: "§f"
        val arrowDisplayName = if (hideAmount) arrow.arrow else StringUtils.pluralize(amount, arrow.arrow)

        if (config.showIcon) {
            add(Renderable.itemStack(itemStack,1.68))
        }
        if (!hideAmount) {
            add(Renderable.string(" §b${amount}x"))
        }
        add(Renderable.string(" $rarity$arrowDisplayName"))
    }

    @SubscribeEvent
    fun onQuiverUpdate(event: QuiverUpdateEvent) {
        arrow = event.currentArrow
        amount = event.currentAmount
        hideAmount = event.hideAmount
        updateDisplay()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        if (display.isEmpty()) return
        if (config.onlyWithBow && !QuiverAPI.hasBowInInventory()) {
            if (display.isNotEmpty()) display = emptyList()
            return
        }
        config.quiverDisplayPos.renderStringsAndItems(listOf(display), posLabel = "Quiver Display")
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
