package at.hannibal2.skyhanni.mixins.transformers.render;

import at.hannibal2.skyhanni.features.gui.StatusEffectHider;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class MixinInGameHud {

    // The status effect in the inventory.
    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void hideStatusEffects(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (StatusEffectHider.shouldHide()) ci.cancel();
    }
}
