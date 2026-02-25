package at.hannibal2.skyhanni.mixins.transformers.renderer;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.renderer.rendertype.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiBufferSource.BufferSource.class)
public class MixinVertexConsumerProvider {

    @Shadow
    protected RenderType lastSharedType;

    @Inject(method = "getBuffer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch(Lnet/minecraft/client/renderer/rendertype/RenderType;Lcom/mojang/blaze3d/vertex/BufferBuilder;)V", shift = At.Shift.BEFORE), cancellable = true)
    private void onGetBuffer(RenderType renderType, CallbackInfoReturnable<VertexConsumer> cir, @Local BufferBuilder bufferBuilder) {
        if (!renderType./*? if < 1.21.11 {*/ getName() /*?} else {*/ /*name *//*?}*/.contains("skyhanni")) {
            return;
        }
        if (renderType./*? if < 1.21.11 {*/ getName() /*?} else {*/ /*name *//*?}*/.equals(this.lastSharedType./*? if < 1.21.11 {*/ getName() /*?} else {*/ /*name *//*?}*/)) {
            cir.setReturnValue(bufferBuilder);
        }
    }

}
