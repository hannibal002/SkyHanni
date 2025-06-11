package at.hannibal2.skyhanni.mixins.transformers.render;

import at.hannibal2.skyhanni.mixins.hooks.RendererLivingEntityHook;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinRendererLivingEntity<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>>
    extends EntityRenderer<T, S>
    implements FeatureRendererContext<S, M> {

    protected MixinRendererLivingEntity(EntityRendererFactory.Context dontCare) {
        super(dontCare);
    }

    @Inject(method = "shouldFlipUpsideDown", at = @At("HEAD"), cancellable = true)
    private static void shouldFlipUpsideDown(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof PlayerEntity || entity.hasCustomName()) {
            if (RendererLivingEntityHook.shouldBeUpsideDown(Formatting.strip(entity.getName().getString()))) {
                cir.setReturnValue(true);
            }
        }
    }
}
