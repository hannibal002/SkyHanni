package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.misc.EmojiReplacer;
import at.hannibal2.skyhanni.features.misc.visualwords.ModifyVisualWords;
import at.hannibal2.skyhanni.utils.compat.OrderedTextUtils;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextRenderer.class)
public class MixinTextRenderer {

    @ModifyVariable(
        //#if MC < 1.21.7
        method = "drawInternal(Lnet/minecraft/text/OrderedText;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;IIZ)I",
        //#else
        //$$ method = "prepare(Lnet/minecraft/text/OrderedText;FFIZI)Lnet/minecraft/client/font/TextRenderer$GlyphDrawable;",
        //#endif
        index = 1,
        at = @At("HEAD"),
        argsOnly = true
    )
    private OrderedText modifyOrderedText(OrderedText value) {

        OrderedText replaced = ModifyVisualWords.INSTANCE.transformText(
            value
        );

        if (replaced == null) return value;
        return replaced;
    }

    @ModifyVariable(
        //#if MC < 1.21.7
        method = "drawInternal(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;IIZ)I",
        //#else
        //$$ method = "prepare(Ljava/lang/String;FFIZI)Lnet/minecraft/client/font/TextRenderer$GlyphDrawable;",
        //#endif
        index = 1,
        at = @At("HEAD"),
        argsOnly = true
    )
    private String modifyString(String value) {

        OrderedText replaced = ModifyVisualWords.INSTANCE.transformText(
            OrderedTextUtils.legacyTextToOrderedText(value)
        );

        if (replaced == null) return value;
        return OrderedTextUtils.orderedTextToLegacyString(replaced);
    }

    @ModifyVariable(
        method = "getWidth(Lnet/minecraft/text/OrderedText;)I",
        index = 1,
        at = @At("HEAD"),
        argsOnly = true
    )
    private OrderedText modifyWidth(OrderedText value) {

        OrderedText replaced = ModifyVisualWords.INSTANCE.transformText(
            value
        );

        if (replaced == null) return value;
        return replaced;
    }

    @ModifyVariable(
        method = "getWidth(Ljava/lang/String;)I",
        index = 1,
        at = @At("HEAD"),
        argsOnly = true
    )
    private String modifyWidth(String value) {

        OrderedText replaced = ModifyVisualWords.INSTANCE.transformText(
            OrderedTextUtils.legacyTextToOrderedText(value)
        );

        if (replaced == null) return value;
        return OrderedTextUtils.orderedTextToLegacyString(replaced);
    }

    @ModifyVariable(
        method = "getWidth(Lnet/minecraft/text/StringVisitable;)I",
        index = 1,
        at = @At("HEAD"),
        argsOnly = true
    )
    private StringVisitable modifyWidth(StringVisitable value) {

        StringVisitable replaced = ModifyVisualWords.INSTANCE.transformStringVisitable(
            value
        );

        if (replaced == null) return value;
        return replaced;
    }

    //#if MC > 1.21.6
    //$$ @Inject(
    //$$     method = "prepare(Ljava/lang/String;FFIZI)Lnet/minecraft/client/font/TextRenderer$GlyphDrawable;",
    //$$     at = @At("TAIL")
    //$$ )
    //$$ public void finish(String string, float x, float y, int color, boolean shadow, int backgroundColor, CallbackInfoReturnable<TextRenderer.GlyphDrawable> cir, @Local TextRenderer.Drawer drawer) {
    //$$     EmojiReplacer.INSTANCE.handleEnd(drawer);
    //$$ }
    //$$ @Inject(
    //$$     method = "prepare(Lnet/minecraft/text/OrderedText;FFIZI)Lnet/minecraft/client/font/TextRenderer$GlyphDrawable;",
    //$$     at = @At("TAIL")
    //$$ )
    //$$ public void finish(OrderedText string, float x, float y, int color, boolean shadow, int backgroundColor, CallbackInfoReturnable<TextRenderer.GlyphDrawable> cir, @Local TextRenderer.Drawer drawer) {
    //$$     EmojiReplacer.INSTANCE.handleEnd(drawer);
    //$$ }
    //#endif
}
