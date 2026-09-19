package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.api.minecraftevents.RenderEvents;
import at.hannibal2.skyhanni.data.ScoreboardData;
import at.hannibal2.skyhanni.events.TitleReceivedEvent;
import at.hannibal2.skyhanni.features.chat.ChatPeek;
import at.hannibal2.skyhanni.utils.compat.TextCompatKt;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 26.2 {
import net.minecraft.client.gui.Hud;
//?} else {
/*import net.minecraft.client.gui.Gui;
*///?}

//~ if < 26.2 'Hud' -> 'Gui'
@Mixin(Hud.class)
public abstract class MixinHud {

    @Inject(method = "extractItemHotbar", at = @At("HEAD"), cancellable = true)
    public void renderHotbar(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (RenderEvents.postHotbarLayerEventPre(context).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractItemHotbar", at = @At("TAIL"))
    public void renderHotbarTail(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        RenderEvents.postHotbarLayerEventPost(context);
    }

    @Inject(method = "extractTabList", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V", shift = At.Shift.BEFORE), cancellable = true)
    public void renderPlayerList(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (RenderEvents.postTablistLayerEventPre(context).isCancelled()) {
            ci.cancel();
        }
    }

    //~ if < 26.2 'ContextualBar' -> 'ContextualBarRenderer' {
    @Inject(method = "extractHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ContextualBar;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", shift = At.Shift.BEFORE), cancellable = true)
    public void renderExperienceBar(GuiGraphicsExtractor context, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (RenderEvents.postExperienceBarLayerEventPre(context).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ContextualBar;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", shift = At.Shift.AFTER))
    public void renderExperienceBarTail(GuiGraphicsExtractor context, DeltaTracker deltaTracker, CallbackInfo ci) {
        RenderEvents.postExperienceBarLayerEventPost(context);
    }

    @Inject(method = "extractHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ContextualBar;extractExperienceLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;I)V", shift = At.Shift.BEFORE), cancellable = true)
    public void renderExperienceLevelHead(GuiGraphicsExtractor context, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (RenderEvents.postExperienceNumberLayerEventPre(context).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractHotbarAndDecorations", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/contextualbar/ContextualBar;extractExperienceLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;I)V", shift = At.Shift.AFTER))
    public void renderExperienceLevelTail(GuiGraphicsExtractor context, DeltaTracker deltaTracker, CallbackInfo ci) {
        RenderEvents.postExperienceNumberLayerEventPost(context);
    }
    //~}

    @ModifyArg(
        method = "displayScoreboardSidebar",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"
        ),
        index = 1
    )
    private Component modifyScoreboardLine(Component str) {
        return ScoreboardData.tryToReplaceScoreboardLine(str);
    }

    @ModifyArg(method = "extractChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V"), index = 5)
    private ChatComponent.DisplayMode modifyRenderText(ChatComponent.DisplayMode mode) {
        if (ChatPeek.peek()) return ChatComponent.DisplayMode.FOREGROUND;
        return mode;
    }

    @WrapMethod(method = "setTitle")
    private void handleTitle(Component component, Operation<Void> original) {
        String formattedText = TextCompatKt.formattedTextCompat(component);
        if (!new TitleReceivedEvent(formattedText, false).post().isCancelled()) {
            original.call(component);
        }
    }

    @WrapMethod(method = "setSubtitle")
    private void handleSubtitle(Component component, Operation<Void> original) {
        String formattedText = TextCompatKt.formattedTextCompat(component);
        if (!new TitleReceivedEvent(formattedText, true).post().isCancelled()) {
            original.call(component);
        }
    }

    @Inject(method = "extractSelectedItemName", at = @At("HEAD"), cancellable = true)
    public void renderSelectedItemNamePre(GuiGraphicsExtractor context, CallbackInfo ci) {
        if (RenderEvents.postHeldItemTooltipLayerEventPre(context).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractSelectedItemName", at = @At("TAIL"))
    public void renderSelectedItemNamePost(GuiGraphicsExtractor context, CallbackInfo ci) {
        RenderEvents.postHeldItemTooltipLayerEventPost(context);
    }

    @Inject(method = "extractOverlayMessage", at = @At("HEAD"), cancellable = true)
    public void renderOverlayMessagePre(GuiGraphicsExtractor context, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (RenderEvents.postActionBarLayerEventPre(context).isCancelled()) {
            ci.cancel();
        }
    }

    // RETURN rather than TAIL: the method returns early when there is no overlay message
    @Inject(method = "extractOverlayMessage", at = @At("RETURN"))
    public void renderOverlayMessagePost(GuiGraphicsExtractor context, DeltaTracker deltaTracker, CallbackInfo ci) {
        RenderEvents.postActionBarLayerEventPost(context);
    }
}
