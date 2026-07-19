package at.hannibal2.skyhanni.mixins.transformers;

//? if >= 26.2 {
import at.hannibal2.skyhanni.data.GuiEditManager;
import at.hannibal2.skyhanni.events.render.gui.GuiScreenOpenEvent;
import at.hannibal2.skyhanni.events.render.gui.RenderingTickEvent;
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat;
import at.hannibal2.skyhanni.utils.system.PlatformUtils;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class MixinGui {

    @Unique
    private GuiGraphicsExtractor skyhanni$graphics;

    @ModifyExpressionValue(
        method = "addInitialScreens",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/Options;onboardAccessibility:Z",
            opcode = Opcodes.GETFIELD
        )
    )
    public boolean onboardAccessibility(boolean original) {
        if (PlatformUtils.isDevEnvironment() && !Boolean.getBoolean("skyhanni.accessibilityOnboarding")) return false;
        return original;
    }

    @Inject(
        method = "setScreen",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/gui/Gui;screen:Lnet/minecraft/client/gui/screens/Screen;",
            opcode = Opcodes.PUTFIELD
        )
    )
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        new GuiScreenOpenEvent(screen).post();
    }

    @ModifyVariable(
        method = "extractRenderState(Lnet/minecraft/client/DeltaTracker;ZZ)V",
        at = @At("STORE"),
        name = "graphics"
    )
    private GuiGraphicsExtractor captureGuiGraphicsExtractor(GuiGraphicsExtractor graphics) {
        skyhanni$graphics = graphics;
        return graphics;
    }

    @Inject(
        method = "extractRenderState(Lnet/minecraft/client/DeltaTracker;ZZ)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Hud;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"
        )
    )
    private void onRenderStartPhase(
        DeltaTracker deltaTracker,
        boolean shouldRenderLevel,
        boolean resourcesLoaded,
        CallbackInfo ci
    ) {
        if (MinecraftCompat.getLocalPlayerExists()) new RenderingTickEvent(skyhanni$graphics, true).post();
    }

    @Inject(
        method = "extractRenderState(Lnet/minecraft/client/DeltaTracker;ZZ)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Hud;extractSavingIndicator(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"
        )
    )
    private void onRenderEndPhase(
        DeltaTracker deltaTracker,
        boolean shouldRenderLevel,
        boolean resourcesLoaded,
        CallbackInfo ci
    ) {
        if (MinecraftCompat.getLocalPlayerExists()) new RenderingTickEvent(skyhanni$graphics, false).post();
    }

    @Inject(
        method = "extractRenderState(Lnet/minecraft/client/DeltaTracker;ZZ)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Hud;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
            shift = At.Shift.AFTER
        )
    )
    private void onRenderTail(
        DeltaTracker deltaTracker,
        boolean shouldRenderLevel,
        boolean resourcesLoaded,
        CallbackInfo ci
    ) {
        GuiEditManager.renderLast(skyhanni$graphics);
    }
}
//?}
