package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.misc.visualwords.ModifyVisualWords;
import at.hannibal2.skyhanni.utils.compat.OrderedTextUtils;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TextRenderer.class)
public class MixinTextRenderer {

    @ModifyVariable(
        //#if MC < 1.21.6
        method = "drawInternal(Lnet/minecraft/text/OrderedText;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;IIZ)I",
        //#else
        //$$ method = "draw(Lnet/minecraft/text/OrderedText;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)V",
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
        //#if MC < 1.21.6
        method = "drawInternal(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;IIZ)I",
        //#else
        //$$ method = "draw(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)V",
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
}
