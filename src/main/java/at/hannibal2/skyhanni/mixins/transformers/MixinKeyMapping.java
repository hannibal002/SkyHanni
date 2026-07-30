package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.model.TextInput;
import at.hannibal2.skyhanni.features.garden.farming.GardenCustomKeybinds;
import at.hannibal2.skyhanni.test.graph.GraphEditor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.ToggleKeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public abstract class MixinKeyMapping {

    @Shadow
    private boolean isDown;

    @SuppressWarnings("FieldCanBeLocal")
    @Mutable
    @Shadow
    private int clickCount;

    @Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
    public void noIsKeyDown(CallbackInfoReturnable<Boolean> cir) {
        @SuppressWarnings("DataFlowIssue")
        KeyMapping keyMapping = (KeyMapping) (Object) this;
        Boolean override = GardenCustomKeybinds.isKeyDown(keyMapping, this.isDown);
        if (override != null)  cir.setReturnValue(override);
        if (keyMapping instanceof ToggleKeyMapping stickyKeyMapping) {
            if (stickyKeyMapping.needsToggle.getAsBoolean()) {
                return;
            }
        }
        if (TextInput.shouldCancelMinecraftInput()) cir.setReturnValue(false);
        if (GraphEditor.shouldCancelMinecraftInput(keyMapping)) cir.setReturnValue(false);
    }

    @Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
    public void noIsPressed(CallbackInfoReturnable<Boolean> cir) {
        @SuppressWarnings("DataFlowIssue")
        KeyMapping keyMapping = (KeyMapping) (Object) this;
        Boolean override = GardenCustomKeybinds.isKeyPressed(keyMapping);
        if (override != null) cir.setReturnValue(override);
        if (cir.isCancelled()) {
            this.clickCount = 0;
        }
        if (keyMapping instanceof ToggleKeyMapping stickyKeyMapping) {
            if (stickyKeyMapping.needsToggle.getAsBoolean()) {
                return;
            }
        }
        if (TextInput.shouldCancelMinecraftInput()) cir.setReturnValue(false);
        if (GraphEditor.shouldCancelMinecraftInput(keyMapping)) cir.setReturnValue(false);
    }
}
