package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.SkyHanniMod;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiIngame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiIngame.class)
public class MixinGuiIngame {

    @Redirect(method = "renderScoreboard", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I"))
    private int renderItemOverlayPost(FontRenderer instance, String text, int x, int y, int color) {
        if (SkyHanniMod.feature.misc.hideScoreboardNumbers) {
            if (text.startsWith("§c") && text.length() <= 4) {
                return 0;
            }
        }

        return instance.drawString(text, x, y, color);
    }
}
