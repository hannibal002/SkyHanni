package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.RenderData;
import at.hannibal2.skyhanni.utils.compat.DrawContext;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.GuiIngameForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngameForge.class)
public class MixinGuiIngameForge {

    @Inject(method = "renderTooltip", at = @At(value = "HEAD"))
    private void onRenderTooltip(ScaledResolution sr, float partialTicks, CallbackInfo ci) {
        RenderData.postRenderOverlay(new DrawContext());
    }
}
