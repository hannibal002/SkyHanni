package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult

object MinecraftInputHook {
    @JvmStatic
    fun shouldCancelMouseRightClick(blockHitResult: HitResult?): Boolean {
        if (blockHitResult == null) return false

        val clickCancelled = ItemClickEvent(InventoryUtils.getItemInHand(), ClickType.RIGHT_CLICK).post()

        val cancelled = when (blockHitResult.type) {
            HitResult.Type.MISS -> {
                clickCancelled
            }

            HitResult.Type.BLOCK -> {
                val position = blockHitResult.location.toLorenzVec()
                BlockClickEvent(
                    ClickType.RIGHT_CLICK,
                    position,
                    InventoryUtils.getItemInHand(),
                ).also {
                    if (clickCancelled) it.cancel()
                }.post()
            }

            HitResult.Type.ENTITY -> {
                EntityClickEvent(
                    ClickType.RIGHT_CLICK,
                    ServerboundInteractPacket.ActionType.INTERACT_AT,
                    (blockHitResult as EntityHitResult).entity,
                    InventoryUtils.getItemInHand(),
                ).also {
                    if (clickCancelled) it.cancel()
                }.post()
            }
        }

        return cancelled
    }

    @JvmStatic
    fun shouldCancelMouseLeftClick(blockHitResult: HitResult?): Boolean {
        if (blockHitResult == null) return false

        val clickCancelled = ItemClickEvent(InventoryUtils.getItemInHand(), ClickType.LEFT_CLICK).post()

        val cancelled = when (blockHitResult.type) {
            HitResult.Type.MISS -> {
                clickCancelled
            }

            HitResult.Type.BLOCK -> {
                val position = (blockHitResult as BlockHitResult).blockPos
                BlockClickEvent(
                    ClickType.LEFT_CLICK,
                    position.toLorenzVec(),
                    InventoryUtils.getItemInHand(),
                ).also {
                    if (clickCancelled) it.cancel()
                }.post()
            }

            HitResult.Type.ENTITY -> {
                EntityClickEvent(
                    ClickType.LEFT_CLICK,
                    ServerboundInteractPacket.ActionType.ATTACK,
                    (blockHitResult as EntityHitResult).entity,
                    InventoryUtils.getItemInHand(),
                ).also {
                    if (clickCancelled) it.cancel()
                }.post()
            }
        }

        return cancelled
    }

    @JvmStatic
    fun shouldCancelContinuedBlockBreak(
        blockHitResult: HitResult?,
        currentBlockPos: BlockPos,
    ): Boolean {
        if (blockHitResult == null || blockHitResult.type != HitResult.Type.BLOCK) return false

        val position = (blockHitResult as BlockHitResult).blockPos

        if (currentBlockPos == position) return false

        val clickCancelled = ItemClickEvent(InventoryUtils.getItemInHand(), ClickType.LEFT_CLICK).post()

        val cancelled = BlockClickEvent(
            ClickType.LEFT_CLICK,
            position.toLorenzVec(),
            InventoryUtils.getItemInHand(),
        ).also {
            if (clickCancelled) it.cancel()
        }.post()


        return cancelled
    }
}
