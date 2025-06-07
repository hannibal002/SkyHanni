package at.hannibal2.skyhanni.mixins.transformers.render;

import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHookKt;
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinRendererLivingEntity<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>>
    extends EntityRenderer<T, S>
    implements FeatureRendererContext<S, M> {

    protected MixinRendererLivingEntity(EntityRendererFactory.Context dontCare) {
        super(dontCare);
    }

    @Inject(method = "getMixColor", at = @At("HEAD"), cancellable = true)
    private void setColorMultiplier(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
        if (EntityRenderDispatcherHookKt.getEntity() instanceof LivingEntity livingEntity) {
            cir.setReturnValue(RenderLivingEntityHelper.internalSetColorMultiplier(livingEntity));
        }
    }

    @Redirect(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;hurtTime:I", opcode = Opcodes.GETFIELD))
    private int changeHurtTime(LivingEntity entity) {
        return RenderLivingEntityHelper.internalChangeHurtTime(entity);
    }

}
