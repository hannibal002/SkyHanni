package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.GuiPlayerTabOverlayHookKt;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(GuiPlayerTabOverlay.class)
public class MixinGuiPlayerTabOverlay {

    @Inject(method = "getPlayerName", at = @At(value = "HEAD"), cancellable = true)
    private void renderItemOverlayPost(PlayerListEntry info, CallbackInfoReturnable<String> cir) {
        String text;
        if (info.getDisplayName() != null) {
            text = info.getDisplayName().formattedTextCompat();
        } else {
            text = Team.formatPlayerName(info.getScoreboardTeam(), info.getProfile().getName());
        }
        GuiPlayerTabOverlayHookKt.getPlayerName(text, cir);
    }
}
