package at.hannibal2.skyhanni.mixins.transformers.render;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VertexConsumerProvider.Immediate.class)
public class MixinVertexConsumerProvider {

    @Shadow
    protected RenderLayer currentLayer;

    @Inject(method = "getBuffer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;draw(Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/BufferBuilder;)V", shift = At.Shift.BEFORE), cancellable = true)
    private void onGetBuffer(RenderLayer renderLayer, CallbackInfoReturnable<VertexConsumer> cir, @Local BufferBuilder bufferBuilder) {
        if (!renderLayer.getName().contains("skyhanni")) {
            return;
        }
        if (renderLayer.getName().equals(currentLayer.getName())) {
            cir.setReturnValue(bufferBuilder);
        }
    }

}
