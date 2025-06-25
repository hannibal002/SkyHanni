package at.hannibal2.skyhanni.mixins.transformers.render;

import at.hannibal2.skyhanni.features.gui.StatusEffectHider;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.StatusEffectsDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StatusEffectsDisplay.class)
public class MixinStatusEffectsDisplay {

    // The status effect in the top right corner of the screen
    @Inject(method = "drawStatusEffects(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("HEAD"), cancellable = true)
    private void hideInventoryStatusEffects(DrawContext context, int mouseX, int mouseY, float tickDelta, CallbackInfo ci) {
        if (StatusEffectHider.shouldHide()) ci.cancel();
    }
}
