package at.hannibal2.skyhanni.features.event.chocolatefactory.clicks

import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.features.event.chocolatefactory.ChocolateFactoryAPI
import at.hannibal2.skyhanni.utils.CollectionUtils.getOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

object CompactFactoryClick {
    private val config get() = ChocolateFactoryAPI.config

    private var lastClick = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!ChocolateFactoryAPI.inChocolateFactory) return
        if (!config.compactOnClick) return

        val itemStack = event.itemStack
        val lore = itemStack.getLore()
        if (!lore.any { it == "§7§eClick to uncover the meaning of life!" }) return
        if (lastClick.passedSince() >= 1.seconds) return
        val list = mutableListOf<String>()
        list.add(itemStack.name)
        lore.getOrNull(5)?.let {
            list.add(it)
        }
        event.toolTip = list
        return
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {

        if (ChocolateFactoryAPI.inChocolateFactory) {
            if (event.slotId == 13) {
                lastClick = SimpleTimeMark.now()
            }
        }
    }
}
