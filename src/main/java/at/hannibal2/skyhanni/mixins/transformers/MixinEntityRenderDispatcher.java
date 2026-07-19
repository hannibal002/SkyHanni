package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.EntityData;
import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHookKt;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if >= 26.1 {
import net.minecraft.client.renderer.state.level.CameraRenderState;
//?} else {
/*import net.minecraft.client.renderer.state.CameraRenderState;
*///?}

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void shouldRender(
        Entity entity, 
        Frustum camera,
        double camX,
        double camY,
        double camZ,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!EntityData.shouldRender(entity, camX, camY, camZ)) {
            cir.setReturnValue(false);
        }
    }

    @WrapOperation(
        method = "submit",
        at = @At(
            value = "INVOKE",
            //~ if < 26.1 'level/CameraRenderState' -> 'CameraRenderState'
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"
        )
    )
    private <S extends EntityRenderState> void setCurrentEntityStateForSubmit(
        EntityRenderer<?, ? super S> renderer,
        S state,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        CameraRenderState cameraRenderState,
        Operation<Void> original
    ) {
        EntityRenderDispatcherHookKt.setEntity(state);
        try {
            original.call(renderer, state, poseStack, submitNodeCollector, cameraRenderState);
        } finally {
            EntityRenderDispatcherHookKt.clearEntity();
        }
    }
}
