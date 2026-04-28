package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.model.TextInput;
import at.hannibal2.skyhanni.features.garden.farming.GardenCustomKeybinds;
import at.hannibal2.skyhanni.test.graph.GraphEditor;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public class MixinKeyBinding {

    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    public void noIsKeyDown(CallbackInfoReturnable<Boolean> cir) {
        KeyMapping keyBinding = (KeyMapping) (Object) this;
        TextInput.Companion.onMinecraftInput(keyBinding, cir);
        GraphEditor.INSTANCE.onMinecraftInput(keyBinding, cir);
    }

    @Inject(method = "set", at = @At("HEAD"), cancellable = true)
    private static void noSet(InputConstants.Key key, boolean pressed, CallbackInfo ci) {
        if (GardenCustomKeybinds.shouldCancelKeyInput(key, pressed)) {
            ci.cancel();
        }
    }

    @Inject(method = "click", at = @At("HEAD"), cancellable = true)
    private static void noClick(InputConstants.Key key, CallbackInfo ci) {
        if (GardenCustomKeybinds.shouldCancelKeyClick(key)) {
            ci.cancel();
        }
    }

    @Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
    public void noIsPressed(CallbackInfoReturnable<Boolean> cir) {
        KeyMapping keyBinding = (KeyMapping) (Object) this;
        TextInput.Companion.onMinecraftInput(keyBinding, cir);
        GraphEditor.INSTANCE.onMinecraftInput(keyBinding, cir);
    }
}
