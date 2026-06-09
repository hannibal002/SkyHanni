package at.hannibal2.skyhanni.mixins.transformers.gui;

import at.hannibal2.skyhanni.api.minecraftevents.RenderEvents;
import at.hannibal2.skyhanni.events.TitleReceivedEvent;
import at.hannibal2.skyhanni.features.chat.ChatPeek;
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard;
import at.hannibal2.skyhanni.mixins.hooks.GuiIngameHook;
import at.hannibal2.skyhanni.utils.compat.TextCompatKt;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
//~ if < 26.2 'net.minecraft.client.gui.Hud' -> 'net.minecraft.client.gui.Gui'
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 26.1
import net.minecraft.client.gui.components.ChatComponent;

//~ if < 26.2 'Hud.class' -> 'Gui.class'
@Mixin(Hud.class)
public abstract class MixinGui {

    @Inject(method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/scores/Objective;)V", at = @At("HEAD"), cancellable = true)
    public void renderScoreboard(GuiGraphicsExtractor graphics, Objective objective, CallbackInfo ci) {
        if (CustomScoreboard.isHideVanillaScoreboardEnabled()) {
            ci.cancel();
        }
    }

    //~ if < 26.1 'extractItemHotbar' -> 'renderItemHotbar' {
    @Inject(method = "extractItemHotbar", at = @At("HEAD"), cancellable = true)
    public void renderHotbar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (RenderEvents.postHotbarLayerEventPre(graphics)) {
            ci.cancel();
        }
    }

    @Inject(method = "extractItemHotbar", at = @At("TAIL"))
    public void renderHotbarTail(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        RenderEvents.postHotbarLayerEventPost(graphics);
    }
    //~}

    //~ if < 26.1 'extractTabList' -> 'renderTabList'
    @WrapOperation(method = "extractTabList", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V"))
    public void renderPlayerList(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Operation<Void> original) {
        if (RenderEvents.postTablistLayerEventPre(graphics)) return;
        original.call(graphics, deltaTracker);
    }

    //? if >= 26.2 {
    @WrapOperation(method = "extractHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ContextualBar;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    //?} else if >= 26.1 {
    /*@WrapOperation(method = "extractHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    *///?} else {
    //@WrapOperation(method = "renderHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    //?}
    public void renderExperienceBar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Operation<Void> original) {
        if (RenderEvents.postExperienceBarLayerEventPre(graphics)) return;
        original.call(graphics, deltaTracker);
        RenderEvents.postExperienceBarLayerEventPost(graphics);
    }

    //? if >= 26.2 {
    @WrapOperation(method = "extractHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ContextualBar;extractExperienceLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;I)V"))
    //?} else if >= 26.1 {
    /*@WrapOperation(method = "extractHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;extractExperienceLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;I)V"))
    *///?} else {
    //@WrapOperation(method = "renderHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;extractExperienceLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;I)V"))
    //?}
    public void renderExperienceLevel(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, Operation<Void> original) {
        if (RenderEvents.postExperienceNumberLayerEventPre(graphics)) return;
        original.call(graphics, deltaTracker);
        RenderEvents.postExperienceNumberLayerEventPost(graphics);
    }

    @Redirect(method = "displayScoreboardSidebar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"))
    private void renderItemOverlayPost(GuiGraphicsExtractor graphics, Font textRenderer, Component text, int x, int y, int color, boolean bl) {
        GuiIngameHook.drawString(textRenderer, graphics, text, x, y, color, bl);
    }

    //? if >= 26.1 {
    @ModifyArg(method = "extractChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V"), index = 5)
    private ChatComponent.DisplayMode modifyRenderText(ChatComponent.DisplayMode mode) {
        if (ChatPeek.peek()) return ChatComponent.DisplayMode.FOREGROUND;
        return mode;
    }
    //?} else {
    /*@ModifyArg(method = "renderChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIIZZ)V"), index = 5)
    private boolean modifyRenderText(boolean isChatting) {
        if (ChatPeek.peek()) return true;
        return isChatting;
    }
    *///?}

    @WrapMethod(method = "setTitle")
    private void handleTitle(Component component, Operation<Void> original) {
        String formattedText = TextCompatKt.formattedTextCompat(component);
        if (!new TitleReceivedEvent(formattedText, false).post()) {
            original.call(component);
        }
    }

    @WrapMethod(method = "setSubtitle")
    private void handleSubtitle(Component component, Operation<Void> original) {
        String formattedText = TextCompatKt.formattedTextCompat(component);
        if (!new TitleReceivedEvent(formattedText, true).post()) {
            original.call(component);
        }
    }

    //~ if < 26.1 '"extractSelectedItemName"' -> '"renderSelectedItemName"' {
    @Inject(method = "extractSelectedItemName", at = @At("HEAD"), cancellable = true)
    public void renderSelectedItemNamePre(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (RenderEvents.postHeldItemTooltipLayerEventPre(graphics)) {
            ci.cancel();
        }
    }

    @Inject(method = "extractSelectedItemName", at = @At("TAIL"))
    public void renderSelectedItemNamePost(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        RenderEvents.postHeldItemTooltipLayerEventPost(graphics);
    }
    //~}

    //~ if < 26.1 '"extractOverlayMessage"' -> '"renderOverlayMessage"' {
    @Inject(method = "extractOverlayMessage", at = @At("HEAD"), cancellable = true)
    public void renderOverlayMessagePre(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (RenderEvents.postActionBarLayerEventPre(graphics)) {
            ci.cancel();
        }
    }

    @Inject(method = "extractOverlayMessage", at = @At("TAIL"))
    public void renderOverlayMessagePost(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        RenderEvents.postActionBarLayerEventPost(graphics);
    }
    //~}
}
