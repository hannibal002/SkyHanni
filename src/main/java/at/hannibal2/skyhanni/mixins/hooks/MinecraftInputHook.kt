package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.hit.HitResult

//#if MC > 1.21
import net.minecraft.util.hit.EntityHitResult
//#endif

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
                val position = blockHitResult.pos.toLorenzVec()
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
                    PlayerInteractEntityC2SPacket.InteractType.INTERACT_AT,
                    //#if MC < 1.21
                    //$$ blockHitResult.entityHit,
                    //#else
                    (blockHitResult as EntityHitResult).getEntity(),
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
    fun shouldCancelMouseLeftClick(blockHitResult: HitResult?): Boolean {
        if (blockHitResult == null) return false

        val clickCancelled = ItemClickEvent(InventoryUtils.getItemInHand(), ClickType.LEFT_CLICK).post()

        val cancelled = when (blockHitResult.type) {
            HitResult.Type.MISS -> {
                clickCancelled
            }

            HitResult.Type.BLOCK -> {
                val position = blockHitResult.pos.toLorenzVec()
                BlockClickEvent(
                    ClickType.LEFT_CLICK,
                    position,
                    InventoryUtils.getItemInHand(),
                ).also {
                    if (clickCancelled) it.cancel()
                }.post()
            }

            HitResult.Type.ENTITY -> {
                EntityClickEvent(
                    ClickType.LEFT_CLICK,
                    PlayerInteractEntityC2SPacket.InteractType.ATTACK,
                    //#if MC < 1.21
                    //$$ blockHitResult.entityHit,
                    //#else
                    (blockHitResult as EntityHitResult).getEntity(),
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
        blockHitResult: HitResult?,
        currentBlockPos: BlockPos
    ): Boolean {
        if (blockHitResult == null || blockHitResult.type != HitResult.Type.BLOCK) return false

        val position = blockHitResult.pos

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
