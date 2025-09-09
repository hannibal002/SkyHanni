package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.PostEffectProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldRenderer.class)
public interface CustomRenderGlobal {
    @Accessor("entityOutlineFramebuffer")
    Framebuffer getEntityOutlineFramebuffer_skyhanni();

    @Accessor("entityOutlineShader")
    PostEffectProcessor getEntityOutlineShader_skyhanni();

}