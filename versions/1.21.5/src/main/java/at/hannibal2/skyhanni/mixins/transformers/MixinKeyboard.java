package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.minecraft.KeyDownEvent;
import at.hannibal2.skyhanni.events.minecraft.KeyUpEvent;
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class MixinKeyboard {

    @Inject(method = "onKey", at = @At("HEAD"))
    private void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (MinecraftClient.getInstance().player == null) return;
        //System.out.println("Key: " + key + " Scancode: " + scancode + " Action: " + action + " Modifiers: " + modifiers);
        /*
            * action = 0: Key released
            * action = 1: Key pressed
            * action = 2: Key held
            * key = keycode
            * not entirely sure what scancode means
            * modifiers = 0: No modifier
            * modifiers = 1: Shift
            * modifiers = 2: Control
            * modifiers = 4: Alt
         */
        // todo on 1.8 it first checks TextInput.isActive() before posting, however im not sure if this is needed
        // and as of now that file would need to be recoded to work with 1.21 so it hasn't been put here
        // there is also an onChar method we could mixin to and use for typing fields and replace TextInput.isActive() with that somehow
        // the extension functions such as isActive() and isKeyHeld() still work from keyboard manager
        // this only replaces the posting of events
        if (action == 0) new KeyUpEvent(key).post();
        if (action == 1) {
            new KeyDownEvent(key).post();
            // on 1.21 it takes like 1 full second before the key press event will get posted so im doing it here
            new KeyPressEvent(key).post();
        }
        if (action == 2) new KeyPressEvent(key).post();
    }
}
