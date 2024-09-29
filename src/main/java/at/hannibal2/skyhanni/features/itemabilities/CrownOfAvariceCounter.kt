package at.hannibal2.skyhanni.features.itemabilities

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.extraAttributes
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getCoinsOfAvarice
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object CrownOfAvariceCounter {

    private val config get() = SkyHanniMod.feature.inventory.itemAbilities.crownOfAvarice

    private val internalName = "CROWN_OF_AVARICE".asInternalName()

    private var render: Renderable? = null

    @SubscribeEvent
    fun onOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if(!isEnabled()) return
        render?.let { config.position.renderRenderable(it, posLabel = "Crown of Avarice Counter") }
    }

    @SubscribeEvent
    fun onInventoryUpdated(event: OwnInventoryItemUpdateEvent) {
        if (!isEnabled() || event.slot != 5) return
        val item = InventoryUtils.getHelmet() ?: return
        if (item.getInternalNameOrNull() != internalName) return
        val count = item.getCoinsOfAvarice() ?: return
        render = Renderable.horizontalContainer(
            listOf(
                Renderable.itemStack(internalName.getItemStack()),
                Renderable.string("§6" + if (config.shortFormat) count.shortFormat() else count.addSeparators()),
            ),
        )
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enable
}
