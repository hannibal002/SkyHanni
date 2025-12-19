package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.RenderGlobalHook;
import at.hannibal2.skyhanni.utils.render.ModernGlStateManager;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public abstract class MixinRenderGlobal {

    @Shadow
    abstract boolean isRenderEntityOutlines();

    @Unique
    private final RenderGlobalHook skyHanni$hook = new RenderGlobalHook();

    @Redirect(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;isRenderEntityOutlines()Z"))
    public boolean renderEntitiesOutlines(LevelRenderer self, Entity renderViewEntity, Frustum camera, float partialTicks) {
        return skyHanni$hook.renderEntitiesOutlines(camera, partialTicks) && this.isRenderEntityOutlines();
    }

    @Inject(method = "isRenderEntityOutlines", at = @At(value = "HEAD"), cancellable = true)
    public void isRenderEntityOutlinesWrapper(CallbackInfoReturnable<Boolean> cir) {
        skyHanni$hook.shouldRenderEntityOutlines(cir);
    }

    @Inject(method = "renderEntityOutlineFramebuffer", at = @At(value = "RETURN"))
    public void afterFramebufferDraw(CallbackInfo callbackInfo) {
        ModernGlStateManager.enableDepthTest();
    }
}
