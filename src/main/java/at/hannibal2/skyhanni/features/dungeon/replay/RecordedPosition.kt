package at.hannibal2.skyhanni.features.dungeon.replay

import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose
import net.minecraft.world.InteractionHand

data class RecordedPosition(
    @Expose val position: LorenzVec,
    @Expose val rotation: Vector2,
    @Expose val limbSwing: Float,
    @Expose val isCrouching: Boolean,
    @Expose val isRiding: Boolean,
    @Expose val interactionHand: InteractionHand,
    @Expose val heldItemID: NeuInternalName? = null,
    @Expose val itemEnchanted: Boolean,
    @Expose val isUsingItem: Boolean,

//     @Expose val position: LorenzVec,
//     @Expose val yaw: Float,
//     @Expose val pitch: Float,
//     @Expose val limbSwing: Float,
//     @Expose val limbSwingAmount: Float,
//     @Expose val swingProgress: Float,
//     @Expose val heldItemID: NeuInternalName?,
//     @Expose val itemEnchanted: Boolean,
//     @Expose val isHoldingItem: Boolean,
//     @Expose val isUsingItem: Boolean,
//     @Expose val isEating: Boolean,
//     @Expose val isSneaking: Boolean,
//     @Expose val isRiding: Boolean
)
