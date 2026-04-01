package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.render.gui.GuiScreenOpenEvent;
import at.hannibal2.skyhanni.utils.TimeUtils;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @ModifyReturnValue(method = "getFps", at = @At("RETURN"))
    private int getFps(int original) {
        return TimeUtils.isAprilFoolsDay() ? original / 2 : original;
    }

    @Inject(method = "setScreen", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;"))
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        new GuiScreenOpenEvent(screen).post();
    }
}
