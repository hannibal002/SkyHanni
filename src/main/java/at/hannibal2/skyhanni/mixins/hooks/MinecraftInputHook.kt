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
//$$ import net.minecraft.util.hit.BlockHitResult
//$$ import net.minecraft.util.hit.EntityHitResult
//#endif

object MinecraftInputHook {
    @JvmStatic
    fun shouldCancelMouseRightClick(hitResult: MovingObjectPosition?): Boolean =
        handleClick(
            hitResult,
            ClickType.RIGHT_CLICK,
            C02PacketUseEntity.Action.INTERACT_AT,
        )

    @JvmStatic
    fun shouldCancelMouseLeftClick(hitResult: MovingObjectPosition?): Boolean =
        handleClick(
            hitResult,
            ClickType.LEFT_CLICK,
            C02PacketUseEntity.Action.ATTACK,
        )

    @JvmStatic
    fun shouldCancelContinuedBlockBreak(
        hitResult: MovingObjectPosition?,
        currentBlockPos: BlockPos,
    ): Boolean {
        if (hitResult == null || hitResult.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return false

        val position = hitResult.getBlockPosCompat()

        if (currentBlockPos == position) return false

        val itemInHand = InventoryUtils.getItemInHand()

        val clickCancelled = ItemClickEvent(itemInHand, ClickType.LEFT_CLICK).post()

        return BlockClickEvent(
            ClickType.LEFT_CLICK,
            position.toLorenzVec(),
            itemInHand,
        ).also {
            if (clickCancelled) it.cancel()
        }.post()
    }

    private fun handleClick(
        hitResult: MovingObjectPosition?,
        clickType: ClickType,
        entityAction: C02PacketUseEntity.Action,
    ): Boolean {
        if (hitResult == null) return false

        val itemInHand = InventoryUtils.getItemInHand()

        val clickCancelled = ItemClickEvent(itemInHand, clickType).post()

        return when (hitResult.typeOfHit) {
            MovingObjectPosition.MovingObjectType.MISS ->
                clickCancelled

            MovingObjectPosition.MovingObjectType.BLOCK -> {
                val pos = hitResult.getBlockPosCompat()
                BlockClickEvent(
                    clickType,
                    pos.toLorenzVec(),
                    itemInHand,
                ).also {
                    if (clickCancelled) it.cancel()
                }.post()
            }

            MovingObjectPosition.MovingObjectType.ENTITY -> {
                EntityClickEvent(
                    clickType,
                    entityAction,
                    hitResult.getEntityCompat(),
                    itemInHand,
                ).also {
                    if (clickCancelled) it.cancel()
                }.post()
            }
        }
    }

    private fun MovingObjectPosition.getBlockPosCompat(): BlockPos =
        //#if MC < 1.21
        this.blockPos
    //#else
    //$$ (blockHitResult as BlockHitResult).blockPos
    //#endif

    private fun MovingObjectPosition.getEntityCompat() =
        //#if MC < 1.21
        this.entityHit
    //#else
    //$$ (this as EntityHitResult).getEntity()
    //#endif
}
