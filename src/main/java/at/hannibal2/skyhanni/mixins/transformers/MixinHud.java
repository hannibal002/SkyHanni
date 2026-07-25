package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.api.minecraftevents.RenderEvents;
import at.hannibal2.skyhanni.data.ScoreboardData;
import at.hannibal2.skyhanni.events.TitleReceivedEvent;
import at.hannibal2.skyhanni.features.chat.ChatPeek;
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard;
import at.hannibal2.skyhanni.utils.compat.TextCompatKt;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >= 26.2 {
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.contextualbar.ContextualBar;
//?} else {
/*import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
*///?}

//~ if < 26.2 'Hud.class' -> 'Gui.class'
@Mixin(Hud.class)
public abstract class MixinHud {

    @Inject(
        method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/scores/Objective;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    public void renderScoreboard(GuiGraphicsExtractor graphics, Objective objective, CallbackInfo ci) {
        if (CustomScoreboard.isHideVanillaScoreboardEnabled()) {
            ci.cancel();
        }
    }

    //~ if < 26.1 'extract' -> 'render'
    @Inject(method = "extractItemHotbar", at = @At("HEAD"), cancellable = true)
    public void renderHotbar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (RenderEvents.postHotbarLayerEventPre(graphics).isCancelled()) {
            ci.cancel();
        }
    }

    //~ if < 26.1 'extract' -> 'render'
    @Inject(method = "extractItemHotbar", at = @At("TAIL"))
    public void renderHotbarTail(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        RenderEvents.postHotbarLayerEventPost(graphics);
    }

    @WrapOperation(
        //~ if < 26.1 'extract' -> 'render'
        method = "extractTabList",
        at = @At(
            value = "INVOKE",
            //~ if < 26.1 'extractRenderState' -> 'render'
            target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;ILnet/minecraft/world/scores/Scoreboard;Lnet/minecraft/world/scores/Objective;)V"
        )
    )
    public void renderPlayerList(
        PlayerTabOverlay tabList,
        GuiGraphicsExtractor graphics,
        int screenWidth,
        Scoreboard scoreboard,
        Objective displayObjective,
        Operation<Void> original
    ) {
        if (RenderEvents.postTablistLayerEventPre(graphics).isCancelled()) return;
        original.call(tabList, graphics, screenWidth, scoreboard, displayObjective);
    }

    @WrapOperation(
        //~ if < 26.1 'extract' -> 'render'
        method = "extractHotbarAndDecorations",
        at = @At(value = "INVOKE",
            //~ if < 26.2 'ContextualBar' -> 'ContextualBarRenderer'
            //~ if < 26.1 'extractBackground' -> 'renderBackground'
            target = "Lnet/minecraft/client/gui/contextualbar/ContextualBar;extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"
        )
    )
    public void renderExperienceBar(
        //~ if < 26.2 'ContextualBar' -> 'ContextualBarRenderer'
        ContextualBar contextualBar,
        GuiGraphicsExtractor graphics,
        DeltaTracker deltaTracker,
        Operation<Void> original
    ) {
        if (RenderEvents.postExperienceBarLayerEventPre(graphics).isCancelled()) return;
        original.call(contextualBar, graphics, deltaTracker);
        RenderEvents.postExperienceBarLayerEventPost(graphics);
    }

    @WrapOperation(
        //~ if < 26.1 'extract' -> 'render'
        method = "extractHotbarAndDecorations",
        at = @At(
            value = "INVOKE",
            //~ if < 26.2 'ContextualBar' -> 'ContextualBarRenderer'
            //~ if < 26.1 'extractExperienceLevel' -> 'renderExperienceLevel'
            target = "Lnet/minecraft/client/gui/contextualbar/ContextualBar;extractExperienceLevel(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;I)V"
        )
    )
    public void renderExperienceLevel(
        GuiGraphicsExtractor graphics,
        Font font,
        int experienceLevel,
        Operation<Void> original
    ) {
        if (RenderEvents.postExperienceNumberLayerEventPre(graphics).isCancelled()) return;
        original.call(graphics, font, experienceLevel);
        RenderEvents.postExperienceNumberLayerEventPost(graphics);
    }

    @WrapOperation(
        method = "displayScoreboardSidebar",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"
        )
    )
    private void modifyScoreboardLine(
        GuiGraphicsExtractor graphics,
        Font font,
        Component str,
        int x,
        int y,
        int color,
        boolean dropShadow,
        Operation<Void> original
    ) {
        Component modifiedStr = ScoreboardData.tryToReplaceScoreboardLine(str);
        original.call(graphics, font, modifiedStr, x, y, color, dropShadow);
    }

    @ModifyArg(
        //~ if < 26.1 'extract' -> 'render'
        method = "extractChat",
        at = @At(
            value = "INVOKE",
            //? if >= 26.1 {
            target = "Lnet/minecraft/client/gui/components/ChatComponent;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V"
            //?} else {
            /*target = "Lnet/minecraft/client/gui/components/ChatComponent;render(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIIZZ)V"
            *///?}
        ),
        index = 5
    )
    //~ if < 26.1 'ChatComponent.DisplayMode' -> 'boolean'
    private ChatComponent.DisplayMode modifyRenderText(ChatComponent.DisplayMode mode) {
        //~ if < 26.1 'ChatComponent.DisplayMode.FOREGROUND' -> 'true'
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

    //~ if < 26.1 'extract' -> 'render'
    @Inject(method = "extractSelectedItemName", at = @At("HEAD"), cancellable = true)
    public void renderSelectedItemNamePre(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (RenderEvents.postHeldItemTooltipLayerEventPre(graphics).isCancelled()) {
            ci.cancel();
        }
    }

    //~ if < 26.1 'extract' -> 'render'
    @Inject(method = "extractSelectedItemName", at = @At("TAIL"))
    public void renderSelectedItemNamePost(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        RenderEvents.postHeldItemTooltipLayerEventPost(graphics);
    }

    //~ if < 26.1 'extract' -> 'render'
    @Inject(method = "extractOverlayMessage", at = @At("HEAD"), cancellable = true)
    public void renderOverlayMessagePre(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (RenderEvents.postActionBarLayerEventPre(graphics).isCancelled()) {
            ci.cancel();
        }
    }

    //~ if < 26.1 'extract' -> 'render'
    @Inject(method = "extractOverlayMessage", at = @At("TAIL"))
    public void renderOverlayMessagePost(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        RenderEvents.postActionBarLayerEventPost(graphics);
    }
}
