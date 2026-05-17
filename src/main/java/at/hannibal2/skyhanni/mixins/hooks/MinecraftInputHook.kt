package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.data.InteractClickType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult

object MinecraftInputHook {
    @JvmStatic
    fun shouldCancelMouseRightClick(hitResult: HitResult?): Boolean =
        handleClick(
            hitResult,
            InteractClickType.RIGHT_CLICK,
            EntityClickEvent.ActionType.INTERACT_AT,
        )

    @JvmStatic
    fun shouldCancelMouseLeftClick(hitResult: HitResult?): Boolean =
        handleClick(
            hitResult,
            InteractClickType.LEFT_CLICK,
            EntityClickEvent.ActionType.ATTACK,
        )

    @JvmStatic
    fun shouldCancelContinuedBlockBreak(
        hitResult: HitResult?,
        currentBlockPos: BlockPos,
    ): Boolean {
        if (hitResult == null || hitResult.type != HitResult.Type.BLOCK) return false

        val position = (hitResult as BlockHitResult).blockPos

        if (currentBlockPos == position) return false

        val clickCancelled = ItemClickEvent(InventoryUtils.getItemInHand(), InteractClickType.LEFT_CLICK).post()

        return BlockClickEvent(
            InteractClickType.LEFT_CLICK,
            position.toLorenzVec(),
            InventoryUtils.getItemInHand(),
        ).also {
            if (clickCancelled) it.cancel()
        }.post()
    }

    private fun handleClick(
        hitResult: HitResult?,
        clickType: InteractClickType,
        entityAction: EntityClickEvent.ActionType,
    ): Boolean {
        if (hitResult == null) return false

        val itemInHand = InventoryUtils.getItemInHand()

        val clickCancelled = ItemClickEvent(itemInHand, clickType).post()

        return when (hitResult.type) {
            HitResult.Type.MISS ->
                clickCancelled

            HitResult.Type.BLOCK -> {
                val pos = (hitResult as BlockHitResult).blockPos
                BlockClickEvent(
                    clickType,
                    pos.toLorenzVec(),
                    itemInHand,
                ).also {
                    if (clickCancelled) it.cancel()
                }.post()
            }

            HitResult.Type.ENTITY -> {
                EntityClickEvent(
                    clickType,
                    entityAction,
                    (hitResult as EntityHitResult).entity,
                    itemInHand,
                ).also {
                    if (clickCancelled) it.cancel()
                }.post()
            }
        }
    }
}
