package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.mixins.hooks.GlowingStateStore;
import at.hannibal2.skyhanni.utils.render.SkyHanniOutlineVertexConsumerProvider;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ItemFeatureRenderer.class)
public class MixinItemFeatureRenderer {

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;setColor(I)V"))
    private void setSkyHanniOutlineColor(OutlineBufferSource outlineConsumer, int i, Operation<Void> original, @Local SubmitNodeStorage.ItemSubmit itemCommand) {
        Object obj = (Object) itemCommand;
        if (obj instanceof GlowingStateStore casted && casted.skyhanni$isUsingCustomOutline()) {
            original.call(SkyHanniOutlineVertexConsumerProvider.getVertexConsumers(), i);
        } else {
            original.call(outlineConsumer, i);
        }
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderItem(Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II[ILjava/util/List;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V", ordinal = 1), index = 2)
    private MultiBufferSource modifyOutlineVertexConsumerProvider(MultiBufferSource outlineConsumer, @Local SubmitNodeStorage.ItemSubmit itemCommand) {
        Object obj = (Object) itemCommand;
        if (obj instanceof GlowingStateStore casted && casted.skyhanni$isUsingCustomOutline()) {
            return SkyHanniOutlineVertexConsumerProvider.getVertexConsumers();
        } else {
            return outlineConsumer;
        }
    }
}
