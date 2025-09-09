package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.RendererLivingEntityHook;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderLivingBase.class)
public class MixinRendererLivingEntity {

    @Redirect(method = "setScoreTeamColor", at = @At(value = "INVOKE", target = "Lat/hannibal2/skyhanni/utils/render/ModernGlStateManager;color(FFFF)V"))
    public void setOutlineColor(float colorRed, float colorGreen, float colorBlue, float colorAlpha, LivingEntity entity) {
        RendererLivingEntityHook.setOutlineColor(colorRed, colorGreen, colorBlue, colorAlpha, entity);
    }
}
