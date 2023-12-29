package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.WorldClickEvent
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PrivateIslandNoPickaxeAbility {

    val config get() = SkyHanniMod.feature.mining

    @SubscribeEvent
    fun onClick(event: WorldClickEvent) {
        if (!LorenzUtils.inSkyBlock || !IslandType.PRIVATE_ISLAND.isInIsland()) return
        if (!config.privateIslandNoPickaxeAbility) return
        if (event.clickType != ClickType.RIGHT_CLICK) return

        when (event.itemInHand?.getItemCategoryOrNull()) {
            ItemCategory.GAUNTLET, ItemCategory.PICKAXE, ItemCategory.DRILL -> {
                event.isCanceled = true
            }

            else -> {}
        }
    }
}
