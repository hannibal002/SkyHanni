package at.hannibal2.skyhanni.mixins.transformers.gui;

import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard;
import net.minecraft.client.gui.GuiIngame;
//#if MC < 1.21
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.ScoreObjective;
//#else
//$$ import net.minecraft.client.gui.DrawContext;
//$$ import net.minecraft.scoreboard.ScoreboardObjective;
//#endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngame.class)
public class MixinGuiIngame {
    @Inject(method =
        //#if MC < 1.21
        "renderScoreboard",
        //#else
        //$$ "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
        //#endif
        at = @At("HEAD"), cancellable = true)
    public void renderScoreboard(
        //#if MC < 1.21
        ScoreObjective objective, ScaledResolution scaledRes, CallbackInfo ci
        //#else
        //$$ DrawContext drawContext, ScoreboardObjective objective, CallbackInfo ci
        //#endif
    ) {
        if (CustomScoreboard.isHideVanillaScoreboardEnabled()) ci.cancel();
    }
}
