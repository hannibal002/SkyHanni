package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard

class TrophyFishFillet {

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (event.slot.inventory.name.contains("Sack")) return
        val internalName = event.itemStack.getInternalName()
        val trophyFishName = internalName.substringBeforeLast("_")
            .replace("_", "").lowercase()
        val trophyRarityName = internalName.substringAfterLast("_")
        val info = TrophyFishManager.getInfo(trophyFishName) ?: return
        val rarity = TrophyRarity.getByName(trophyRarityName) ?: return
        val multiplier = if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) event.itemStack.stackSize else 1
        val filletValue = info.getFilletValue(rarity) * multiplier
        val filletPrice = filletValue * NEUItems.getPrice("MAGMA_FISH")
        event.toolTip.add("§7Fillet: §8${filletValue.addSeparators()} Magmafish §7(§6${NumberUtil.format(filletPrice)}§7)")
    }
}