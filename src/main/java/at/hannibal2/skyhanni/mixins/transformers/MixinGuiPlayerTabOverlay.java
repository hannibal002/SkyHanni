package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.GuiPlayerTabOverlayHookKt;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.scores.PlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(GuiPlayerTabOverlay.class)
public class MixinGuiPlayerTabOverlay {

    @Inject(method = "getPlayerName", at = @At(value = "HEAD"), cancellable = true)
    private void renderItemOverlayPost(PlayerInfo info, CallbackInfoReturnable<String> cir) {
        String text;
        if (info.getTabListDisplayName() != null) {
            text = info.getTabListDisplayName().formattedTextCompat();
        } else {
            text = PlayerTeam.formatPlayerName(info.getTeam(), info.getProfile().getName());
        }
        GuiPlayerTabOverlayHookKt.getPlayerName(text, cir);
    }
}
