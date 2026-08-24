package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.render.gui.GuiScreenOpenEvent;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 26.2 {
import net.minecraft.client.gui.Gui;
//?} else {
/*import net.minecraft.client.Minecraft;
*///?}

//~ if < 26.2 'Gui' -> 'Minecraft'
@Mixin(Gui.class)
public abstract class MixinGui_Minecraft {

    //~ if < 26.2 'gui/Gui' -> 'Minecraft'
    @Inject(method = "setScreen", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Gui;screen:Lnet/minecraft/client/gui/screens/Screen;"))
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        new GuiScreenOpenEvent(screen).post();
    }
}
