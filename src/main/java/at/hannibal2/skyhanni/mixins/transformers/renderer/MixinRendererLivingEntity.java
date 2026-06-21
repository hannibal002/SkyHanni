package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.data.entity.EntityTransparencyManager;
import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHookKt;
import at.hannibal2.skyhanni.mixins.hooks.RendererLivingEntityHook;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.renderer.rendertype.RenderTypes;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinRendererLivingEntity<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>>
    extends EntityRenderer<T, S>
    implements RenderLayerParent<S, M> {

    @Shadow
    public abstract Identifier getTextureLocation(LivingEntityRenderState par1);

    protected MixinRendererLivingEntity(EntityRendererProvider.Context dontCare) {
        super(dontCare);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At(value = "TAIL"))
    public void updateRenderState(LivingEntity livingEntity, LivingEntityRenderState livingEntityRenderState, float f, CallbackInfo ci) {
        if (livingEntity instanceof Player playerEntity) {
            Float yaw = RendererLivingEntityHook.rotatePlayer(playerEntity);
            if (yaw != null) {
                livingEntityRenderState.bodyRot = yaw;
            }
        }
    }


    @ModifyArg(
        //~ if < 26.1 'state/level/CameraRenderState;' -> 'state/CameraRenderState;'
        method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At(value = "INVOKE",target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"),
        index = 6
    )
    private int modifyRenderAlpha(int argb) {
        if (EntityRenderDispatcherHookKt.getEntity() instanceof LivingEntity livingEntity) {
            Integer entityAlpha = EntityTransparencyManager.getEntityTransparency(livingEntity);
            if (entityAlpha == null) return argb;

            int oldAlpha = (argb >> 24) & 0xFF;
            int newAlpha = Math.min(oldAlpha, entityAlpha);

            argb &= 0xFFFFFF;
            argb |= newAlpha << 24;
        }
        return argb;
    }

    @WrapWithCondition(
        //~ if < 26.1 'state/level/CameraRenderState;' -> 'state/CameraRenderState;'
        method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V")
    )
    private boolean shouldSubmitEntityModel(
        SubmitNodeCollector submitNodeCollector,
        Model<?> model,
        Object state,
        PoseStack poseStack,
        RenderType renderType,
        int lightCoords,
        int overlayCoords,
        int color,
        TextureAtlasSprite sprite,
        int outlineColor,
        ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        return !(state instanceof LivingEntityRenderState livingState &&
            livingState.isInvisible &&
            livingState.skyhanni$isUsingCustomOutline());
    }

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    public void getRenderState(LivingEntityRenderState state, boolean showBody, boolean translucent, boolean showOutline, CallbackInfoReturnable<RenderType> cir) {
        if (showBody && EntityRenderDispatcherHookKt.getEntity() instanceof LivingEntity livingEntity) {
            if (EntityTransparencyManager.getEntityTransparency(livingEntity) == null) return;
            //~ if < 26.1 'entityTranslucentCullItemTarget' -> 'itemEntityTranslucentCull'
            cir.setReturnValue(RenderTypes.entityTranslucentCullItemTarget(this.getTextureLocation(state)));
        }
    }

}
