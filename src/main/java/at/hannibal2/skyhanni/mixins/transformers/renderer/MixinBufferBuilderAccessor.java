package at.hannibal2.skyhanni.mixins.transformers.renderer;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BufferBuilder.class)
public interface MixinBufferBuilderAccessor {
    @Invoker("beginElement")
    long invokeBeginElement(VertexFormatElement element);
}
