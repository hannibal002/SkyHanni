package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.client.renderer.LevelRenderer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface CustomRenderGlobal {
    @Accessor("entityOutlineTarget")
    RenderTarget getEntityOutlineFramebuffer_skyhanni();

    @Accessor("entityOutlineShader")
    PostChain getEntityOutlineShader_skyhanni();

}