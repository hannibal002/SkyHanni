package at.hannibal2.skyhanni.mixins.hooks

import net.minecraft.client.Minecraft

class EntityRendererHook {
    companion object {

        @JvmStatic
        fun onTranslateCamera(x: Float, y: Float, z: Float) {
            if (Minecraft.getMinecraft().thePlayer.isSneaking()) {
                println("z: $z")
            }
        }
    }
}
