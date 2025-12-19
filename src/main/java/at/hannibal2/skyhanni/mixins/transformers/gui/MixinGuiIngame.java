package at.hannibal2.skyhanni.mixins.transformers.gui;

import at.hannibal2.skyhanni.api.minecraftevents.RenderEvents;
import at.hannibal2.skyhanni.features.chat.ChatPeek;
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard;
import at.hannibal2.skyhanni.mixins.hooks.GuiIngameHook;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.scores.Objective;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class MixinGuiIngame {

    @Inject(method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/scores/Objective;)V", at = @At("HEAD"), cancellable = true)
    public void renderScoreboard(GuiGraphics drawContext, Objective objective, CallbackInfo ci) {
        if (CustomScoreboard.isHideVanillaScoreboardEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderItemHotbar", at = @At("HEAD"), cancellable = true)
    public void renderHotbar(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (RenderEvents.postHotbarLayerEventPre(context)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderItemHotbar", at = @At("TAIL"))
    public void renderHotbarTail(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        RenderEvents.postHotbarLayerEventPost(context);
    }

    @Inject(method = "renderTabList", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;render(Lnet/minecraft/client/gui/GuiGraphics;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V", shift = At.Shift.BEFORE), cancellable = true)
    public void renderPlayerList(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (RenderEvents.postTablistLayerEventPre(context)) {
            ci.cancel();
        }
    }

    //#if MC < 1.21.6
    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    public void renderExperienceBar(GuiGraphics context, int x, CallbackInfo ci) {
        if (RenderEvents.postExperienceBarLayerEventPre(context)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderExperienceBar", at = @At("TAIL"))
    public void renderExperienceBarTail(GuiGraphics context, int x, CallbackInfo ci) {
        RenderEvents.postExperienceBarLayerEventPost(context);
    }

    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
    public void renderExperienceLevel(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (RenderEvents.postExperienceNumberLayerEventPre(context)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderExperienceLevel", at = @At("TAIL"))
    public void renderExperienceLevelTail(GuiGraphics context, DeltaTracker tickCounter, CallbackInfo ci) {
        RenderEvents.postExperienceNumberLayerEventPost(context);
    }

    @Redirect(method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/scores/Objective;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I"))
    private int drawScoreboardString(GuiGraphics drawContext, Font textRenderer, Component text, int x, int y, int color, boolean shadow) {
        return GuiIngameHook.drawString(textRenderer, drawContext, text, x, y, color);
    }
    //#else
    //$$ @Inject(method = "renderHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", shift = At.Shift.BEFORE), cancellable = true)
    //$$ public void renderExperienceBar(GuiGraphics context, DeltaTracker deltaTracker, CallbackInfo ci) {
    //$$     if (RenderEvents.postExperienceBarLayerEventPre(context)) {
    //$$         ci.cancel();
    //$$     }
    //$$ }
    //$$
    //$$ @Inject(method = "renderHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", shift = At.Shift.AFTER))
    //$$ public void renderExperienceBarTail(GuiGraphics context, DeltaTracker deltaTracker, CallbackInfo ci) {
    //$$     RenderEvents.postExperienceBarLayerEventPost(context);
    //$$ }
    //$$
    //$$ @Inject(method = "renderHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;renderExperienceLevel(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;I)V", shift = At.Shift.BEFORE), cancellable = true)
    //$$ public void renderExperienceLevel(GuiGraphics context, DeltaTracker deltaTracker, CallbackInfo ci) {
    //$$     if (RenderEvents.postExperienceNumberLayerEventPre(context)) {
    //$$         ci.cancel();
    //$$     }
    //$$ }
    //$$
    //$$ @Inject(method = "renderHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;renderExperienceLevel(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;I)V", shift = At.Shift.AFTER))
    //$$ public void renderExperienceLevelTail(GuiGraphics context, DeltaTracker deltaTracker, CallbackInfo ci) {
    //$$     RenderEvents.postExperienceNumberLayerEventPost(context);
    //$$ }
    //$$
    //$$ @Redirect(method = "displayScoreboardSidebar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"))
    //$$ private void renderItemOverlayPost(GuiGraphics drawContext, Font textRenderer, Component text, int x, int y, int color, boolean bl) {
    //$$     GuiIngameHook.drawString(textRenderer, drawContext, text, x, y, color);
    //$$ }
    //#endif

    @ModifyArg(method = "renderChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;render(Lnet/minecraft/client/gui/GuiGraphics;IIIZ)V"))
    private boolean modifyRenderText(boolean bool) {
        if (ChatPeek.peek()) return true;
        return bool;
    }

}
