package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.model.TextInput;
import at.hannibal2.skyhanni.features.garden.farming.GardenCustomKeybinds;
import at.hannibal2.skyhanni.test.graph.GraphEditor;
import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#if MC > 1.21
//$$ import net.minecraft.client.option.StickyKeyBinding;
//#endif

@Mixin(KeyBinding.class)
public class MixinKeyBinding {

    @Inject(method = "isKeyDown", at = @At("HEAD"), cancellable = true)
    public void noIsKeyDown(CallbackInfoReturnable<Boolean> cir) {
        KeyBinding keyBinding = (KeyBinding) (Object) this;
        GardenCustomKeybinds.isKeyDown(keyBinding, cir);
        //#if MC > 1.21
        //$$ if (keyBinding instanceof StickyKeyBinding stickyKeyBinding) {
        //$$     if (stickyKeyBinding.toggleGetter.getAsBoolean()) {
        //$$         return;
        //$$     }
        //$$ }
        //#endif
        TextInput.Companion.onMinecraftInput(keyBinding, cir);
        GraphEditor.INSTANCE.onMinecraftInput(keyBinding, cir);
    }

    @Inject(method = "isPressed", at = @At("HEAD"), cancellable = true)
    public void noIsPressed(CallbackInfoReturnable<Boolean> cir) {
        KeyBinding keyBinding = (KeyBinding) (Object) this;
        GardenCustomKeybinds.isKeyPressed(keyBinding, cir);
        //#if MC > 1.21
        //$$ if (keyBinding instanceof StickyKeyBinding stickyKeyBinding) {
        //$$     if (stickyKeyBinding.toggleGetter.getAsBoolean()) {
        //$$         return;
        //$$     }
        //$$ }
        //#endif
        TextInput.Companion.onMinecraftInput(keyBinding, cir);
        GraphEditor.INSTANCE.onMinecraftInput(keyBinding, cir);
    }
}
