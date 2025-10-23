package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.util.BlockPos
import net.minecraft.util.MovingObjectPosition

//#if MC > 1.21
//$$ import net.minecraft.util.hit.EntityHitResult
//#endif

object MinecraftInputHook {
    @JvmStatic
    fun shouldCancelMouseRightClick(blockHitResult: MovingObjectPosition?): Boolean {
        if (blockHitResult == null) return false

        val clickCancelled = ItemClickEvent(InventoryUtils.getItemInHand(), ClickType.RIGHT_CLICK).post()

        val cancelled = when (blockHitResult.typeOfHit) {
            MovingObjectPosition.MovingObjectType.MISS -> {
                clickCancelled
            }

            MovingObjectPosition.MovingObjectType.BLOCK -> {
                val position = blockHitResult.blockPos.toLorenzVec()
                BlockClickEvent(
                    ClickType.RIGHT_CLICK,
                    position,
                    InventoryUtils.getItemInHand(),
                ).also {
                    if (clickCancelled) it.cancel()
                }.post()
            }

            MovingObjectPosition.MovingObjectType.ENTITY -> {
                EntityClickEvent(
                    ClickType.RIGHT_CLICK,
                    C02PacketUseEntity.Action.INTERACT_AT,
                    //#if MC < 1.21
                    blockHitResult.entityHit,
                    //#else
                    //$$ (blockHitResult as EntityHitResult).getEntity(),
                    //#endif
                    InventoryUtils.getItemInHand(),
                ).also {
                    if (clickCancelled) it.cancel()
                }.post()
            }
        }

        return cancelled
    }

    @JvmStatic
    fun shouldCancelMouseLeftClick(blockHitResult: MovingObjectPosition?): Boolean {
        if (blockHitResult == null) return false

        val clickCancelled = ItemClickEvent(InventoryUtils.getItemInHand(), ClickType.LEFT_CLICK).post()

        val cancelled = when (blockHitResult.typeOfHit) {
            MovingObjectPosition.MovingObjectType.MISS -> {
                clickCancelled
            }

            MovingObjectPosition.MovingObjectType.BLOCK -> {
                val position = blockHitResult.blockPos.toLorenzVec()
                BlockClickEvent(
                    ClickType.LEFT_CLICK,
                    position,
                    InventoryUtils.getItemInHand(),
                ).also {
                    if (clickCancelled) it.cancel()
                }.post()
            }

            MovingObjectPosition.MovingObjectType.ENTITY -> {
                EntityClickEvent(
                    ClickType.LEFT_CLICK,
                    C02PacketUseEntity.Action.ATTACK,
                    //#if MC < 1.21
                    blockHitResult.entityHit,
                    //#else
                    //$$ (blockHitResult as EntityHitResult).getEntity(),
                    //#endif
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
        blockHitResult: MovingObjectPosition?,
        currentBlockPos: BlockPos
    ): Boolean {
        if (blockHitResult == null || blockHitResult.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return false

        val position = blockHitResult.blockPos

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
