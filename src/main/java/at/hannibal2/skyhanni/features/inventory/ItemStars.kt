package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.inventory.InventoryConfig.ItemNumberEntry.CRIMSON_ARMOR
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.features.inventory.ItemDisplayOverlayFeatures.isSelected
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraAPI.getKuudraTier
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraAPI.isKuudraArmor
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getDungeonStarCount
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object ItemStars {

    private val config get() = SkyHanniMod.feature.inventory

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!isEnabled()) return
        val stack = event.itemStack
        if (stack.stackSize != 1) return
        val stars = stack.getStarCount() ?: return
        val name = stack.name.substringBefore('✪').trim()
        event.toolTip[0] = "$name §c$stars✪"
    }

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!CRIMSON_ARMOR.isSelected()) return
        val stack = event.stack
        if (stack.getInternalNameOrNull()?.isKuudraArmor() != true) return
        val stars = stack.getStarCount() ?: return
        event.stackTip = stars.toString()
    }

    fun ItemStack.getStarCount(): Int? {
        val internalName = getInternalNameOrNull() ?: return null
        val baseStars = getDungeonStarCount() ?: getStarCount() ?: return null
        if (internalName.isKuudraArmor()) {
            val tier = internalName.getKuudraTier() ?: return baseStars
            return baseStars + tier * 10
        }
        return baseStars
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.itemStars
}
