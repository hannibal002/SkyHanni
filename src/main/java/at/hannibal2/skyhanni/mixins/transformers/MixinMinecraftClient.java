package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.render.gui.GuiScreenOpenEvent;
//~ if < 26.2 'net.minecraft.client.gui.Gui' -> 'net.minecraft.client.Minecraft'
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//~ if < 26.2 'Gui.class' -> 'Minecraft.class'
@Mixin(Gui.class)
public abstract class MixinMinecraftClient {

    //~ if < 26.2 'Lnet/minecraft/client/gui/Gui;screen:' -> 'Lnet/minecraft/client/Minecraft;screen:'
    @Inject(method = "setScreen", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Gui;screen:Lnet/minecraft/client/gui/screens/Screen;"))
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        new GuiScreenOpenEvent(screen).post();
    }
}
