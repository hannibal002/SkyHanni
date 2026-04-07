package at.hannibal2.skyhanni.mixins.transformers.gui;

import at.hannibal2.skyhanni.api.minecraftevents.RenderEvents;
import at.hannibal2.skyhanni.events.TitleReceivedEvent;
import at.hannibal2.skyhanni.features.chat.ChatPeek;
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard;
import at.hannibal2.skyhanni.mixins.hooks.GuiIngameHook;
import at.hannibal2.skyhanni.utils.compat.TextCompatKt;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if > 1.21.11
//import net.minecraft.client.gui.components.ChatComponent;

@Mixin(Gui.class)
public class MixinGui {

    @Inject(method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/scores/Objective;)V", at = @At("HEAD"), cancellable = true)
    public void renderScoreboard(GuiGraphics drawContext, Objective objective, CallbackInfo ci) {
        if (CustomScoreboard.isHideVanillaScoreboardEnabled()) {
            ci.cancel();
        }
    }

    //~ if > 1.21.11 'renderItemHotbar' -> 'extractItemHotbar'
    @Inject(method = "renderItemHotbar", at = @At("HEAD"), cancellable = true)
    public void renderHotbar(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (RenderEvents.postHotbarLayerEventPre(context)) {
            ci.cancel();
        }
    }

    //~ if > 1.21.11 'renderItemHotbar' -> 'extractItemHotbar'
    @Inject(method = "renderItemHotbar", at = @At("TAIL"))
    public void renderHotbarTail(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        RenderEvents.postHotbarLayerEventPost(context);
    }

    //~ if > 1.21.11 'renderTabList' -> 'extractTabList'
    //~ if > 1.21.11 ';render(' -> ';extractRenderState('
    @Inject(method = "renderTabList", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V", shift = At.Shift.BEFORE), cancellable = true)
    public void renderPlayerList(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (RenderEvents.postTablistLayerEventPre(context)) {
            ci.cancel();
        }
    }

    //~ if > 1.21.11 'renderHotbarAndDecorations' -> 'extractHotbarAndDecorations'
    @Inject(method = "renderHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", shift = At.Shift.BEFORE), cancellable = true)
    public void renderExperienceBar(GuiGraphics context, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (RenderEvents.postExperienceBarLayerEventPre(context)) {
            ci.cancel();
        }
    }

    //~ if > 1.21.11 'renderHotbarAndDecorations' -> 'extractHotbarAndDecorations'
    @Inject(method = "renderHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", shift = At.Shift.AFTER))
    public void renderExperienceBarTail(GuiGraphics context, DeltaTracker deltaTracker, CallbackInfo ci) {
        RenderEvents.postExperienceBarLayerEventPost(context);
    }

    //~ if > 1.21.11 'renderHotbarAndDecorations' -> 'extractHotbarAndDecorations'
    //~ if > 1.21.11 ';renderExperienceLevel(' -> ';extractExperienceLevel('
    @Inject(method = "renderHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;renderExperienceLevel(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;I)V", shift = At.Shift.BEFORE), cancellable = true)
    public void renderExperienceLevel(GuiGraphics context, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (RenderEvents.postExperienceNumberLayerEventPre(context)) {
            ci.cancel();
        }
    }

    //~ if > 1.21.11 'renderHotbarAndDecorations' -> 'extractHotbarAndDecorations'
    //~ if > 1.21.11 ';renderExperienceLevel(' -> ';extractExperienceLevel('
    @Inject(method = "renderHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;renderExperienceLevel(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;I)V", shift = At.Shift.AFTER))
    public void renderExperienceLevelTail(GuiGraphics context, DeltaTracker deltaTracker, CallbackInfo ci) {
        RenderEvents.postExperienceNumberLayerEventPost(context);
    }

    //~ if > 1.21.11 'drawString' -> 'text'
    @Redirect(method = "displayScoreboardSidebar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"))
    private void renderItemOverlayPost(GuiGraphics drawContext, Font textRenderer, Component text, int x, int y, int color, boolean bl) {
        GuiIngameHook.drawString(textRenderer, drawContext, text, x, y, color, bl);
    }

    //~ if > 1.21.11 '"renderChat"' -> '"extractChat"'
    //~ if > 1.21.11 ';render(' -> ';extractRenderState('
    //~ if > 1.21.11 'Font;IIIZZ)V' -> 'Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V'
    @ModifyArg(method = "renderChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;IIIZZ)V"), index = 5)
    //~ if > 1.21.11 'private boolean' -> 'private ChatComponent.DisplayMode'
    //~ if > 1.21.11 'boolean bool' -> 'ChatComponent.DisplayMode mode'
    private boolean modifyRenderText(boolean bool) {
        //~ if > 1.21.11 'true' -> 'ChatComponent.DisplayMode.FOREGROUND'
        if (ChatPeek.peek()) return true;
        //~ if > 1.21.11 'bool' -> 'mode'
        return bool;
    }

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

    //~ if > 1.21.11 'renderSelectedItemName' -> 'extractSelectedItemName'
    @Inject(method = "renderSelectedItemName", at = @At("HEAD"), cancellable = true)
    public void renderSelectedItemNamePre(GuiGraphics context, CallbackInfo ci) {
        if (RenderEvents.postHeldItemTooltipLayerEventPre(context)) {
            ci.cancel();
        }
    }

    //~ if > 1.21.11 'renderSelectedItemName' -> 'extractSelectedItemName'
    @Inject(method = "renderSelectedItemName", at = @At("TAIL"))
    public void renderSelectedItemNamePost(GuiGraphics context, CallbackInfo ci) {
        RenderEvents.postHeldItemTooltipLayerEventPost(context);
    }

    //~ if > 1.21.11 'renderOverlayMessage' -> 'extractOverlayMessage'
    @Inject(method = "renderOverlayMessage", at = @At("HEAD"), cancellable = true)
    public void renderOverlayMessagePre(GuiGraphics context, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (RenderEvents.postActionBarLayerEventPre(context)) {
            ci.cancel();
        }
    }

    //~ if > 1.21.11 'renderOverlayMessage' -> 'extractOverlayMessage'
    @Inject(method = "renderOverlayMessage", at = @At("TAIL"))
    public void renderOverlayMessagePost(GuiGraphics context, DeltaTracker deltaTracker, CallbackInfo ci) {
        RenderEvents.postActionBarLayerEventPost(context);
    }
}
