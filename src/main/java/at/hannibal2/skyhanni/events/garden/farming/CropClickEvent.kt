package at.hannibal2.skyhanni.events.garden.farming

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState

/**
 * When the player clicks on a block that is linked to a CropType while in the garden.
 */
class CropClickEvent(
    val position: LorenzVec,
    val crop: CropType,
    val blockState: BlockState,
    val clickType: ClickType,
    val itemInHand: ItemStack?,
) : SkyHanniEvent()
