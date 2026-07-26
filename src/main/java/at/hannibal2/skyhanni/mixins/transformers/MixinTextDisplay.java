package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.TextDisplayHook;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Display.TextDisplay.class)
public abstract class MixinTextDisplay {
    @Inject(method = "setText", at = @At("HEAD"))
    private void onSetText(Component text, CallbackInfo ci) {
        TextDisplayHook.onTextDisplayUpdate(((Display.TextDisplay) (Object) this), text);
    }
}
