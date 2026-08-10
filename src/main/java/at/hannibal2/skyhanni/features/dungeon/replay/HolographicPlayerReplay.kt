package at.hannibal2.skyhanni.features.dungeon.replay

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.HolographicEntities.HolographicEntity
import at.hannibal2.skyhanni.utils.HolographicEntities.renderHolographicEntity
import at.hannibal2.skyhanni.utils.ItemUtils.addEnchantGlint
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import com.mojang.authlib.GameProfile
import net.minecraft.client.Minecraft
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

@SkyHanniModule
object HolographicPlayerReplay {
    private val mc get() = Minecraft.getInstance()
    private val config get() = SkyHanniMod.feature.dev

    fun SkyHanniRenderWorldEvent.renderHolographicEntity(
        holographicEntity: HolographicEntity<AbstractClientPlayer>,
        opacity: Float = 0.3f,
        position: RecordedPosition,
        previousPosition: RecordedPosition,
        index: Int, //used for contrib spin
    ) {
        val newPosition = interpolateRecordedPosition(previousPosition, position, partialTicks)

        val item = position.heldItemID?.getItemStackOrNull()?.let { item ->
            if (item.isEnchanted) item.addEnchantGlint()
            item
        } ?: ItemStack.EMPTY

        holographicEntity.entity.setItemInHand(position.interactionHand, item)
        holographicEntity.moveTo(newPosition.position, newPosition.rotation.y)

        this.renderHolographicEntity(holographicEntity as HolographicEntity<*>, opacity)
    }

    private fun interpolateRecordedPosition(last: RecordedPosition, next: RecordedPosition, progress: Float): RecordedPosition {
        val interpolatedPosition = interpolatePosition(last.position, next.position, progress)
        val interpolatedRotation = interpolateRotation(last.rotation, next.rotation, progress)
        val interpolatedLimbSwing = interpolateValue(last.limbSwing, next.limbSwing, progress)

        return RecordedPosition(
            interpolatedPosition,
            interpolatedRotation,
            interpolatedLimbSwing,
            last.isCrouching,
            last.isRiding,
            last.interactionHand,
            last.heldItemID,
            last.itemEnchanted,
            last.isUsingItem,
        )
    }

    private fun interpolateValue(last: Float, next: Float, progress: Float): Float {
        return last + (next - last) * progress
    }

    private fun interpolatePosition(last: LorenzVec, next: LorenzVec, progress: Float): LorenzVec {
        val x = last.x + (next.x - last.x) * progress
        val y = last.y + (next.y - last.y) * progress
        val z = last.z + (next.z - last.z) * progress
        return LorenzVec(x, y, z)
    }

    private fun interpolateRotation(last: Vector2, next: Vector2, progress: Float): Vector2 {
        fun interpolateRotationValue(last: Float, next: Float): Float {
            var direction: Float = next - last
            while (direction < -180.0f) {
                direction += 360.0f
            }
            while (direction >= 180.0f) {
                direction -= 360.0f
            }
            return last + progress * direction
        }

        return Vector2(interpolateRotationValue(last.x, next.x), interpolateRotationValue(last.y, next.y))
    }
}

