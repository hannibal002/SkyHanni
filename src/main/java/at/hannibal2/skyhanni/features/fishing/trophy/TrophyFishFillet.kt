package at.hannibal2.skyhanni.features.fishing.trophy

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyFishManager.getFilletValue
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard

@SkyHanniModule
object TrophyFishFillet {

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!isEnabled()) return
        if (event.slot.inventory.name.contains("Sack")) return
        val internalName = event.itemStack.getInternalName().asString()
        val trophyFishName = internalName.substringBeforeLast("_")
            .replace("_", "").lowercase()
        val trophyRarityName = internalName.substringAfterLast("_")
        val info = TrophyFishManager.getInfo(trophyFishName) ?: return
        val rarity = TrophyRarity.getByName(trophyRarityName) ?: return
        val multiplier = if (Keyboard.KEY_LSHIFT.isKeyHeld()) event.itemStack.stackSize else 1
        val filletValue = info.getFilletValue(rarity) * multiplier
        // TODO use magma fish member
        val filletPrice = filletValue * "MAGMA_FISH".asInternalName().getPrice()
        event.toolTip.add("§7Fillet: §8${filletValue.addSeparators()} Magmafish §7(§6${NumberUtil.format(filletPrice)}§7)")
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "fishing.trophyFilletTooltip", "fishing.trophyFishing.filletTooltip")
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && SkyHanniMod.feature.fishing.trophyFishing.filletTooltip
}
