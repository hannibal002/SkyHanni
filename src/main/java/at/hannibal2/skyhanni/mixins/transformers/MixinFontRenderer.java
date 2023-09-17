package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.misc.ModifyVisualWords;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FontRenderer.class)
public class MixinFontRenderer {
    @Inject(method = "renderStringAtPos", at = @At("RETURN"), cancellable = true)
    private void modifyRenderedText(String text, boolean shadow, CallbackInfo ci) {

        String modifiedText = ModifyVisualWords.INSTANCE.modifyText(text);

        if (!modifiedText.equals(text)) {

            ci.cancel();
            FontRenderer fontRenderer = (FontRenderer)(Object)this;
            fontRenderer.drawString(modifiedText, 0, 0, 0xFFFFFF, shadow);
        }
    }
}
