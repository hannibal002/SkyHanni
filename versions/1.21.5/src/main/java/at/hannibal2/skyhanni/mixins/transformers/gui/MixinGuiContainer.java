package at.hannibal2.skyhanni.mixins.transformers.gui;


import at.hannibal2.skyhanni.mixins.hooks.GuiContainerHook;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class MixinGuiContainer<T extends ScreenHandler> extends Screen {

    protected MixinGuiContainer(Text title) {
        super(title);
    }

    @Unique
    private final GuiContainerHook skyHanni$hook = new GuiContainerHook(this);

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawSlotHighlightBack(Lnet/minecraft/client/gui/DrawContext;)V"))
    private void backgroundDrawn(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        skyHanni$hook.backgroundDrawn(context, mouseX, mouseY, deltaTicks);
    }

}
