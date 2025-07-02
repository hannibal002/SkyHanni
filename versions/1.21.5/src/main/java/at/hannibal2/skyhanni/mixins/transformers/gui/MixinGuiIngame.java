package at.hannibal2.skyhanni.mixins.transformers.gui;

import at.hannibal2.skyhanni.api.minecraftevents.RenderEvents;
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class MixinGuiIngame {

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("HEAD"), cancellable = true)
    public void renderScoreboard(DrawContext drawContext, ScoreboardObjective objective, CallbackInfo ci) {
        if (CustomScoreboard.isHideVanillaScoreboardEnabled()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    public void renderHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (RenderEvents.postHotbarLayerEventPre(context)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderHotbar", at = @At("TAIL"))
    public void renderHotbarTail(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        RenderEvents.postHotbarLayerEventPost(context);
    }

    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    public void renderExperienceBar(DrawContext context, int x, CallbackInfo ci) {
        if (RenderEvents.postExperienceBarLayerEventPre(context)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderExperienceBar", at = @At("TAIL"))
    public void renderExperienceBarTail(DrawContext context, int x, CallbackInfo ci) {
        RenderEvents.postExperienceBarLayerEventPost(context);
    }

    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
    public void renderExperienceLevel(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (RenderEvents.postExperienceNumberLayerEventPre(context)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderExperienceLevel", at = @At("TAIL"))
    public void renderExperienceLevelTail(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        RenderEvents.postExperienceNumberLayerEventPost(context);
    }

    @Inject(method = "renderPlayerList", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/PlayerListHud;render(Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/scoreboard/Scoreboard;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", shift = At.Shift.BEFORE), cancellable = true)
    public void renderPlayerList(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (RenderEvents.postTablistLayerEventPre(context)) {
            ci.cancel();
        }
    }

}
