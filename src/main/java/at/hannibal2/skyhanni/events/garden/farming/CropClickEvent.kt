package at.hannibal2.skyhanni.events.garden.farming

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SafeItemStack
import net.minecraft.world.level.block.state.BlockState

/**
 * When the player clicks on a block that is linked to a CropType while in the garden.
 */
@PrimaryFunction("onCropClick")
class CropClickEvent(
    blockClickEvent: BlockClickEvent,
    val crop: CropType,
) : SkyHanniEvent() {
    val position: LorenzVec = blockClickEvent.position
    val itemInHand: SafeItemStack? = blockClickEvent.itemInHand
}
