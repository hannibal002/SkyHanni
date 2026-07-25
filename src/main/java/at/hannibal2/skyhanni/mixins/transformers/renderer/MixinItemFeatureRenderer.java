package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.mixins.hooks.GlowingStateStore;
import at.hannibal2.skyhanni.utils.render.SkyHanniOutlineVertexConsumerProvider;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//? if >= 26.1 {
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;
//?} else {
/*import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.injection.ModifyArg;
*///?}

@Mixin(ItemFeatureRenderer.class)
public abstract class MixinItemFeatureRenderer {

    @WrapOperation(
        //? if >= 26.1 {
        method = "renderItem(Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/OutlineBufferSource;Lnet/minecraft/client/renderer/SubmitNodeStorage$ItemSubmit;)V",
        //?} else {
        /*method = "render",
        *///?}
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;setColor(I)V")
    )
    private void setSkyHanniOutlineColor(OutlineBufferSource outlineConsumer, int i, Operation<Void> original, @Local SubmitNodeStorage.ItemSubmit itemCommand) {
        Object obj = itemCommand;
        if (obj instanceof GlowingStateStore casted && casted.skyhanni$isUsingCustomOutline()) {
            original.call(SkyHanniOutlineVertexConsumerProvider.getVertexConsumers(), i);
        } else {
            original.call(outlineConsumer, i);
        }
    }

    //? if >= 26.1 {
    @WrapOperation(
        method = "renderItem(Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/OutlineBufferSource;Lnet/minecraft/client/renderer/SubmitNodeStorage$ItemSubmit;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/OutlineBufferSource;getBuffer(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
        )
    )
    private VertexConsumer modifyOutlineVertexConsumerProvider(OutlineBufferSource outlineConsumer, RenderType renderType, Operation<VertexConsumer> original, @Local(argsOnly = true) SubmitNodeStorage.ItemSubmit itemCommand) {
        Object obj = (Object) itemCommand;
        if (obj instanceof GlowingStateStore casted && casted.skyhanni$isUsingCustomOutline()) {
            return SkyHanniOutlineVertexConsumerProvider.getVertexConsumers().getBuffer(renderType);
        }
        return original.call(outlineConsumer, renderType);
    }
    //?} else {
    /*@ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderItem(Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II[ILjava/util/List;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V",
            ordinal = 1
        ),
        index = 2
    )
    private MultiBufferSource modifyOutlineVertexConsumerProvider(MultiBufferSource outlineConsumer, @Local SubmitNodeStorage.ItemSubmit itemCommand) {
        Object obj = itemCommand;
        if (obj instanceof GlowingStateStore casted && casted.skyhanni$isUsingCustomOutline()) {
            return SkyHanniOutlineVertexConsumerProvider.getVertexConsumers();
        }
        return outlineConsumer;
    }
    *///?}
}
