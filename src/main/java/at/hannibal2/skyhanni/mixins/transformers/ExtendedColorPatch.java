package at.hannibal2.hanni.mixins.transformers;

import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(FontRenderer.class)
public abstract class ExtendedColorPatch {

    @Shadow(remap = false)
    protected abstract void setColor(float r, float g, float b, float a2);

    @Shadow
    private int textColor;
    @Shadow
    private float alpha;
    @Unique
    private int hanni$colorSR;
    @Unique
    private int hanni$colorState;

    @Inject(method = "renderStringAtPos", at = @At("HEAD"))
    private void resetStateWhenRendering(String text, boolean shadow, CallbackInfo ci) {
        hanni$colorSR = 0;
        hanni$colorState = -1;
    }

    @Unique
    private static boolean hanni$isSpecial = false;

    @Inject(
        method = "isFormatSpecial",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    private static void protectFormatCodesSpecial(char formatChar, CallbackInfoReturnable<Boolean> cir) {
        if (formatChar == '/') {
            hanni$isSpecial = false;
            cir.setReturnValue(true);
        } else if (hanni$isSpecial) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
        method = "isFormatColor",
        at = @At(value = "HEAD"),
        cancellable = true
    )
    private static void protectFormatCodesSimple(char formatChar, CallbackInfoReturnable<Boolean> cir) {
        if (formatChar == '#') {
            hanni$isSpecial = true;
            cir.setReturnValue(true);
        } else if (hanni$isSpecial) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "getFormatFromString",
        at = @At(value = "TAIL")
    )
    private static void resetState(String text, CallbackInfoReturnable<String> cir) {
        hanni$isSpecial = false;
    }

    @Inject(
        method = "getFormatFromString",
        at = @At(value = "HEAD")
    )
    private static void resetStateAtHead(String text, CallbackInfoReturnable<String> cir) {
        hanni$isSpecial = false;
    }

    @Inject(
        method = "renderStringAtPos",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;setColor(FFFF)V",
            ordinal = 0,
            shift = At.Shift.AFTER,
            remap = false
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void onChooseColor(
        String text,
        boolean shadow,
        CallbackInfo ci,
        int i,
        char c0,
        int i1
    ) {

        char c = text.charAt(i + 1);
        int hexCode = "0123456789abcdef".indexOf(c);
        if (c == '#') {
            if (hanni$colorState != -1) {
                throw new IllegalStateException("Encountered §# while inside push sequence");
            }
            hanni$colorState = 0;
            hanni$colorSR = 0;
        } else if (c == '/') {
            if (hanni$colorState != 8 && hanni$colorState != 6) {
                throw new IllegalStateException("Encountered §/ without encountering enough pushes: " + hanni$colorState);
            }
            textColor = hanni$colorSR;
            int shadowDivisor = shadow ? 4 : 1;
            setColor(
                (hanni$colorSR >> 16 & 0xFF) / 255f / shadowDivisor,
                (hanni$colorSR >> 8 & 0xFF) / 255f / shadowDivisor,
                (hanni$colorSR & 0xFF) / 255f / shadowDivisor,
                (hanni$colorState == 8 ? (hanni$colorSR >> 24 & 0xFF) / 255f : this.alpha)
            );
            hanni$colorState = -1;
        } else if (0 <= hexCode && hanni$colorState != -1) {
            hanni$colorState++;
            if (hanni$colorState > 8)
                throw new IllegalStateException("Encountered too many pushes inside of §#§/ sequence");
            hanni$colorSR = (hanni$colorSR << 4) | hexCode;
        }
    }

}
