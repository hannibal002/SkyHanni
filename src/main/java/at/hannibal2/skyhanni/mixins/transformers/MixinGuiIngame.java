package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.GuiIngameHook;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiIngame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
//#if MC > 1.21
//$$ import at.hannibal2.skyhanni.features.chat.ChatPeek;
//$$ import net.minecraft.client.gui.DrawContext;
//$$ import net.minecraft.text.Text;
//$$ import org.spongepowered.asm.mixin.injection.ModifyArg;
//#endif

@Mixin(GuiIngame.class)
public class MixinGuiIngame {

    //#if MC < 1.21
    @Redirect(method = "renderScoreboard", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I"))
    private int renderItemOverlayPost(FontRenderer instance, String text, int x, int y, int color) {
        return GuiIngameHook.drawString(instance, text, x, y, color);
    }
    //#else
    //$$ @Redirect(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)I"))
    //$$     private int renderItemOverlayPost(DrawContext drawContext, TextRenderer textRenderer, Text text, int x, int y, int color, boolean shadow) {
    //$$         return GuiIngameHook.drawString(textRenderer, drawContext, text, x, y, color);
    //$$     }
    //$$
    //$$ @ModifyArg(method = "renderChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;render(Lnet/minecraft/client/gui/DrawContext;IIIZ)V"))
    //$$ private boolean modifyRenderText(boolean bool) {
    //$$     if (ChatPeek.peek()) return true;
    //$$     return bool;
    //$$ }
    //#endif


}
