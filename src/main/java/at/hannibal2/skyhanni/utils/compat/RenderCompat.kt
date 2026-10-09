package at.hannibal2.skyhanni.utils.compat

import com.mojang.blaze3d.pipeline.RenderPipeline
import net.minecraft.client.renderer.RenderPipelines

object RenderCompat {
    const val CLEAR_DEPTH = 0.0

    fun getMinecraftGuiTextured(): RenderPipeline = RenderPipelines.GUI_TEXTURED
}
