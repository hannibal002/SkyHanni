package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.GuiEditManager;
import at.hannibal2.skyhanni.events.render.gui.RenderingTickEvent;
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    @Unique
    private GuiGraphicsExtractor skyhanni$guiGraphics;

    @ModifyVariable(
        //~ if < 26.1 'extractGui(Lnet/minecraft/client/DeltaTracker;ZZ)V' -> 'render'
        method = "extractGui(Lnet/minecraft/client/DeltaTracker;ZZ)V",
        at = @At("STORE"),
        //~ if < 26.1 'graphics' -> 'guiGraphics'
        name = "graphics"
    )
    private GuiGraphicsExtractor skyhanni$captureGuiGraphicsExtractor(GuiGraphicsExtractor graphics) {
        skyhanni$guiGraphics = graphics;
        return graphics;
    }

    @Inject(
        //~ if < 26.1 'extractGui(Lnet/minecraft/client/DeltaTracker;ZZ)V' -> 'render'
        method = "extractGui(Lnet/minecraft/client/DeltaTracker;ZZ)V",
        at = @At(
            value = "INVOKE",
            //? if >= 26.1
            target = "Lnet/minecraft/client/gui/Gui;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"
            //? else
            //target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V"
        )
    )
    private void skyhanni$onRenderStartPhase(
        DeltaTracker deltaTracker,
        boolean shouldRenderLevel,
        //? if >= 26.1
        boolean resourcesLoaded,
        CallbackInfo ci
    ) {
        if (MinecraftCompat.getLocalPlayerExists()) new RenderingTickEvent(skyhanni$guiGraphics, true).post();
    }

    @Inject(
        //~ if < 26.1 '"extractGui(Lnet/minecraft/client/DeltaTracker;ZZ)V"' -> '"render"'
        method = "extractGui(Lnet/minecraft/client/DeltaTracker;ZZ)V",
        at = @At(
            value = "INVOKE",
            //~ if < 26.1 'extractSavingIndicator' -> 'renderSavingIndicator'
            target = "Lnet/minecraft/client/gui/Gui;extractSavingIndicator(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"
        )
    )
    private void skyhanni$onRenderEndPhase(
        DeltaTracker deltaTracker,
        boolean shouldRenderLevel,
        //? if >= 26.1
        boolean resourcesLoaded,
        CallbackInfo ci
    ) {
        if (MinecraftCompat.getLocalPlayerExists()) new RenderingTickEvent(skyhanni$guiGraphics, false).post();
    }

    @Inject(
        //~ if < 26.1 '"extractGui(Lnet/minecraft/client/DeltaTracker;ZZ)V"' -> '"render"'
        method = "extractGui(Lnet/minecraft/client/DeltaTracker;ZZ)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
            shift = At.Shift.AFTER
        )
    )
    private void skyhanni$onRenderTail(
        DeltaTracker deltaTracker,
        boolean shouldRenderLevel,
        //? if >= 26.1
        boolean resourcesLoaded,
        CallbackInfo ci
    ) {
        GuiEditManager.renderLast(skyhanni$guiGraphics);
    }
}
