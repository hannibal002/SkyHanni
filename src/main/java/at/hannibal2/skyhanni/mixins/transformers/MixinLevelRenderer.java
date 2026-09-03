package at.hannibal2.skyhanni.mixins.transformers;

//? if < 26.2 {
/*import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper;
import at.hannibal2.skyhanni.mixins.hooks.SkyHanniOutlineHook;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {
    @WrapOperation(method = "extractVisibleEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/state/EntityRenderState;appearsGlowing()Z"))
    public boolean shouldAlsoGlow(EntityRenderState instance, Operation<Boolean> original, @Local Entity entity) {
        Integer glowColor = RenderLivingEntityHelper.getEntityGlowColor(entity);
        return glowColor != null || original.call(instance);
    }

    @Inject(
        method = "lambda$addMainPass$0",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearColorAndDepthTextures(Lcom/mojang/renderpearl/api/textures/GpuTexture;ILcom/mojang/renderpearl/api/textures/GpuTexture;D)V",
            ordinal = 0,
            shift = At.Shift.AFTER
        )
    )
    private void setGlowDepth(CallbackInfo ci) {
        if (!RenderLivingEntityHelper.isUsingCustomGlow()) return;
        SkyHanniOutlineHook.checkIfDepthAttachmentNeedsUpdating();
    }

    @Inject(method = "lambda$addMainPass$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OutlineBufferSource;endOutlineBatch()V"))
    private void renderSkyHanniGlow(CallbackInfo ci) {
        if (!RenderLivingEntityHelper.isUsingCustomGlow()) return;
        SkyHanniOutlineHook.getVertexConsumers().endOutlineBatch();
    }
}
*///?}
