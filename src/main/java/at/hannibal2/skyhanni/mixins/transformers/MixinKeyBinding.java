package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.model.TextInput;
import at.hannibal2.skyhanni.features.garden.farming.GardenCustomKeybinds;
import at.hannibal2.skyhanni.test.graph.GraphEditor;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.ToggleKeyMapping;

@Mixin(KeyMapping.class)
public class MixinKeyBinding {

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
        if (keyBinding instanceof ToggleKeyMapping stickyKeyBinding) {
            if (stickyKeyBinding.needsToggle.getAsBoolean()) {
                return;
            }
        }
        TextInput.Companion.onMinecraftInput(keyBinding, cir);
        GraphEditor.INSTANCE.onMinecraftInput(keyBinding, cir);
    }
}
