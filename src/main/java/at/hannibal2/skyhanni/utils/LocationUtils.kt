package at.hannibal2.skyhanni.utils

import net.minecraft.client.Minecraft

object LocationUtils {

    fun canSee(a: LorenzVec, b: LorenzVec): Boolean {
        return Minecraft.getMinecraft().theWorld.rayTraceBlocks(a.toVec3(), b.toVec3()) == null
    }

    fun playerLocation(): LorenzVec {
        return Minecraft.getMinecraft().thePlayer.getLorenzVec()
    }

    fun playerEyeLocation(): LorenzVec {
        val player = Minecraft.getMinecraft().thePlayer
        val vec = player.getLorenzVec()
        return vec.add(0.0, 0.0 + player.getEyeHeight(), 0.0)
    }
}