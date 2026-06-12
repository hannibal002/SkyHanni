package at.hannibal2.skyhanni.mixins.transformers.renderer;

// TODO 26.2
//? if < 26.2 {
/*import at.hannibal2.skyhanni.mixins.hooks.GlowingStateStore;
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
import at.hannibal2.skyhanni.data.entity.EntityTransparencyManager;
import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHookKt;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.injection.ModifyArg;
//?} else {
/^import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.injection.ModifyArg;
^///?}

@Mixin(ItemFeatureRenderer.class)
public abstract class MixinItemFeatureRenderer {

    @WrapOperation(
        //? if >= 26.1 {
        method = "renderItem(Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/OutlineBufferSource;Lnet/minecraft/client/renderer/SubmitNodeStorage$ItemSubmit;)V",
        //?} else
        //method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;setColor(I)V")
    )
    private void setSkyHanniOutlineColor(
        OutlineBufferSource outlineConsumer,
        int color,
        Operation<Void> original,
        //~ if < 26.1 '"submit"' -> '"itemSubmit"'
        @Local(name = "submit"/^? if >= 26.1 {^/, argsOnly = true/^?}^/) SubmitNodeStorage.ItemSubmit submit
    ) {
        if (submit instanceof GlowingStateStore casted && casted.skyhanni$isUsingCustomOutline()) {
            original.call(SkyHanniOutlineVertexConsumerProvider.getVertexConsumers(), color);
        } else {
            original.call(outlineConsumer, color);
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
    private VertexConsumer modifyOutlineVertexConsumerProvider(
        OutlineBufferSource outlineConsumer,
        RenderType renderType,
        Operation<VertexConsumer> original,
        @Local(argsOnly = true, name = "submit") SubmitNodeStorage.ItemSubmit itemCommand
    ) {
        if ((Object) itemCommand instanceof GlowingStateStore casted && casted.skyhanni$isUsingCustomOutline()) {
            return SkyHanniOutlineVertexConsumerProvider.getVertexConsumers().getBuffer(renderType);
        }
        return original.call(outlineConsumer, renderType);
    }

    @ModifyArg(
        method = "renderItem(Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/OutlineBufferSource;Lnet/minecraft/client/renderer/SubmitNodeStorage$ItemSubmit;)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/QuadInstance;setColor(I)V"),
        index = 0
    )
    private int modifyAlpha(int originalColor) {
        if (!(EntityRenderDispatcherHookKt.getEntity() instanceof LivingEntity livingEntity)) return originalColor;

        Integer entityAlpha = EntityTransparencyManager.getEntityTransparency(livingEntity);
        if (entityAlpha == null) return originalColor;
        int newAlpha = Math.min(ARGB.alpha(originalColor), entityAlpha);
        return ARGB.color(newAlpha, ARGB.red(originalColor), ARGB.green(originalColor), ARGB.blue(originalColor));
    }

    @ModifyExpressionValue(
        method = "renderItem(Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lnet/minecraft/client/renderer/OutlineBufferSource;Lnet/minecraft/client/renderer/SubmitNodeStorage$ItemSubmit;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/geometry/BakedQuad$MaterialInfo;itemRenderType()Lnet/minecraft/client/renderer/rendertype/RenderType;")
    )
    private RenderType modifyRenderLayer(RenderType layer) {
        if (!(EntityRenderDispatcherHookKt.getEntity() instanceof LivingEntity livingEntity)) return layer;
        if (EntityTransparencyManager.getEntityTransparency(livingEntity) == null) return layer;
        return RenderTypes.glintTranslucent();
    }
    //?} else {
    /^@ModifyArg(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderItem(Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II[ILjava/util/List;Lnet/minecraft/client/renderer/rendertype/RenderType;Lnet/minecraft/client/renderer/item/ItemStackRenderState$FoilType;)V",
            ordinal = 1
        ),
        index = 2
    )
    private MultiBufferSource modifyOutlineVertexConsumerProvider(
        MultiBufferSource outlineConsumer,
        //~ if < 26.1 '"submit"' -> '"itemSubmit"'
        @Local(name = "submit") SubmitNodeStorage.ItemSubmit submit
    ) {
        if ((Object) submit instanceof GlowingStateStore casted && casted.skyhanni$isUsingCustomOutline()) {
            return SkyHanniOutlineVertexConsumerProvider.getVertexConsumers();
        }
        return outlineConsumer;
    }
    ^///?}
}
*///?}
