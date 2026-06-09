package at.hannibal2.skyhanni.mixins.transformers.renderer;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BufferBuilder.class)
public interface MixinBufferBuilderAccessor {
    //? if >= 26.2 {
    @Accessor("vertexPointer")
    long getSkyHanniVertexPointer();

    @Accessor("format")
    VertexFormat getSkyHanniFormat();
    //?} else {
    /*@Invoker("beginElement")
    long invokeBeginElement(VertexFormatElement element);
    *///?}
}
