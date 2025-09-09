package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.util.math.Direction
//#if MC > 1.16
import net.minecraft.client.MinecraftClient
//#endif

fun PlayerInteractBlockC2SPacket.getFacing(): Direction =
    //#if MC < 1.16
    //$$ EnumFacing.getFront(placedBlockDirection)
//#else
blockHitResult.side
//#endif

fun PlayerInteractBlockC2SPacket.getUsedItem(): ItemStack? =
    //#if MC < 1.16
    //$$ stack
//#else
MinecraftClient.getInstance().player?.getStackInHand(hand)
//#endif

fun PlayerMoveC2SPacket.getLocation(): LorenzVec =
    //#if MC < 1.16
    //$$ LorenzVec(positionX, positionY, positionZ)
//#else
LorenzVec(getX(0.0), getY(0.0), getZ(0.0))
//#endif
