package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.model.TextInput;
import at.hannibal2.skyhanni.features.garden.farming.GardenCustomKeybinds;
import at.hannibal2.skyhanni.test.graph.GraphEditor;
import at.hannibal2.skyhanni.utils.SkyHanniKeyBindManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.ToggleKeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public class MixinKeyMapping {

    @Mutable
    @Shadow
    private int clickCount;

    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    public void noIsKeyDown(CallbackInfoReturnable<Boolean> cir) {
        KeyMapping keyBinding = (KeyMapping) (Object) this;
        GardenCustomKeybinds.isKeyDown(keyBinding, cir);
        if (keyBinding instanceof ToggleKeyMapping stickyKeyBinding) {
            if (stickyKeyBinding.needsToggle.getAsBoolean()) {
                return;
            }
        }
        TextInput.Companion.onMinecraftInput(keyBinding, cir);
        GraphEditor.INSTANCE.onMinecraftInput(keyBinding, cir);
    }

    @Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
    public void noIsPressed(CallbackInfoReturnable<Boolean> cir) {
        KeyMapping keyBinding = (KeyMapping) (Object) this;
        GardenCustomKeybinds.isKeyPressed(keyBinding, cir);
        if (cir.isCancelled()) {
            this.clickCount = 0;
        }
        if (keyBinding instanceof ToggleKeyMapping stickyKeyBinding) {
            if (stickyKeyBinding.needsToggle.getAsBoolean()) {
                return;
            }
        }
        TextInput.Companion.onMinecraftInput(keyBinding, cir);
        GraphEditor.INSTANCE.onMinecraftInput(keyBinding, cir);
    }

    @Inject(method = "setKey", at = @At("TAIL"))
    public void onSetKey(InputConstants.Key key, CallbackInfo ci) {
        SkyHanniKeyBindManager.onKeyMappingSet((KeyMapping) (Object) this, key);
    }
}
