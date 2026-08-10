package at.hannibal2.skyhanni.features.dungeon.replay

import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName
import com.google.gson.annotations.Expose
import net.minecraft.world.InteractionHand

data class RecordedPositionDelta(
    @Expose val position: LorenzVec? = null,
    @Expose val rotation: Vector2? = null,
    @Expose val limbSwing: Float? = null,
    @Expose val isCrouching: Boolean? = null,
    @Expose val isRiding: Boolean? = null,
    @Expose val interactionHand: InteractionHand? = null,
    @Expose val heldItemID: NeuInternalName? = null,
    @Expose val itemEnchanted: Boolean? = null,
    @Expose val isUsingItem: Boolean? = null,
) {
    companion object {
        fun getComplete(positions: List<RecordedPositionDelta>, index: Int): RecordedPosition {
            var incompletePositions = RecordedPositionDelta()

            for (i in index downTo 0) {
                if (i >= positions.size) continue
                val position = positions[i]

                incompletePositions = incompletePositions.copy(
                    position = incompletePositions.position ?: position.position,
                    rotation = incompletePositions.rotation ?: position.rotation,
                    limbSwing = incompletePositions.limbSwing ?: position.limbSwing,
                    isCrouching = incompletePositions.isCrouching ?: position.isCrouching,
                    isRiding = incompletePositions.isRiding ?: position.isRiding,
                    heldItemID = incompletePositions.heldItemID ?: position.heldItemID,
                    itemEnchanted = incompletePositions.itemEnchanted ?: position.itemEnchanted,
                    isUsingItem = incompletePositions.isUsingItem ?: position.isUsingItem
                )

                if (incompletePositions.isComplete()) break
            }

            return RecordedPosition(
                incompletePositions.position ?: LorenzVec(),
                incompletePositions.rotation ?: Vector2(),
                incompletePositions.limbSwing ?: 0f,
                incompletePositions.isCrouching ?: false,
                incompletePositions.isRiding ?: false,
                incompletePositions.interactionHand ?: InteractionHand.MAIN_HAND,
                incompletePositions.heldItemID,
                incompletePositions.itemEnchanted ?: false,
                incompletePositions.isUsingItem ?: false
            )
        }

        private fun RecordedPositionDelta.isComplete(): Boolean {
            return position != null &&
                rotation != null &&
                limbSwing != null &&
                heldItemID != null &&
                itemEnchanted != null &&
                isUsingItem != null &&
                isCrouching != null &&
                isRiding != null
        }
    }
}
