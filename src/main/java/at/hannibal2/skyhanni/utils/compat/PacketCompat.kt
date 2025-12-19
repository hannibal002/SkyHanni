package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraft.world.item.ItemStack
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.core.Direction
//#if MC > 1.16
import net.minecraft.client.Minecraft
//#endif

fun ServerboundUseItemOnPacket.getFacing(): Direction =
    //#if MC < 1.16
    //$$ EnumFacing.getFront(placedBlockDirection)
//#else
hitResult.direction
//#endif

fun ServerboundUseItemOnPacket.getUsedItem(): ItemStack? =
    //#if MC < 1.16
    //$$ stack
//#else
Minecraft.getInstance().player?.getItemInHand(hand)
//#endif

fun ServerboundMovePlayerPacket.getLocation(): LorenzVec =
    //#if MC < 1.16
    //$$ LorenzVec(positionX, positionY, positionZ)
//#else
LorenzVec(getX(0.0), getY(0.0), getZ(0.0))
//#endif
