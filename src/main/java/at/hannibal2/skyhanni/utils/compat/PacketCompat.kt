package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.EnumFacing
//#if MC > 1.16
//$$ import net.minecraft.client.Minecraft
//#endif

fun C08PacketPlayerBlockPlacement.getFacing(): EnumFacing =
    //#if MC < 1.16
    EnumFacing.getFront(placedBlockDirection)
//#else
//$$ hitResult.direction
//#endif

fun C08PacketPlayerBlockPlacement.getUsedItem(): ItemStack? =
    //#if MC < 1.16
    stack
//#else
//$$ Minecraft.getInstance().player?.getItemInHand(hand)
//#endif

fun C03PacketPlayer.getLocation(): LorenzVec =
    //#if MC < 1.16
    LorenzVec(positionX, positionY, positionZ)
//#else
//$$ LorenzVec(getX(0.0), getY(0.0), getZ(0.0))
//#endif
