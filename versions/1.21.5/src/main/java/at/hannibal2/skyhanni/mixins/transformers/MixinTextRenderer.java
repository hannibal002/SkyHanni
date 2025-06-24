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
        method = "drawInternal(Lnet/minecraft/text/OrderedText;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;IIZ)I",
        index = 1,
        at = @At("HEAD"),
        argsOnly = true
    )
    private OrderedText modifyOrderedText(OrderedText value) {

        String replaced = ModifyVisualWords.INSTANCE.modifyText(
            OrderedTextUtils.orderedTextToLegacyString(value)
        );

        if (replaced == null) return value;
        return OrderedTextUtils.legacyTextToOrderedText(
            replaced
        );
    }

    @ModifyVariable(
        method = "drawInternal(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;IIZ)I",
        index = 1,
        at = @At("HEAD"),
        argsOnly = true
    )
    private String modifyString(String value) {

        String replaced = ModifyVisualWords.INSTANCE.modifyText(
            value
        );

        if (replaced == null) return value;
        return replaced;
    }

    @ModifyVariable(
        method = "getWidth(Lnet/minecraft/text/OrderedText;)I",
        index = 1,
        at = @At("HEAD"),
        argsOnly = true
    )

    private OrderedText modifyWidth(OrderedText value) {

        String replaced = ModifyVisualWords.INSTANCE.modifyText(
            OrderedTextUtils.orderedTextToLegacyString(value)
        );

        if (replaced == null) return value;
        return OrderedTextUtils.legacyTextToOrderedText(
            replaced
        );
    }

    @ModifyVariable(
        method = "getWidth(Ljava/lang/String;)I",
        index = 1,
        at = @At("HEAD"),
        argsOnly = true
    )
    private String modifyWidth(String value) {

        String replaced = ModifyVisualWords.INSTANCE.modifyText(
            value
        );

        if (replaced == null) return value;
        return replaced;
    }

    @ModifyVariable(
        method = "getWidth(Lnet/minecraft/text/StringVisitable;)I",
        index = 1,
        at = @At("HEAD"),
        argsOnly = true
    )
    private StringVisitable modifyWidth(StringVisitable value) {

        String replaced = ModifyVisualWords.INSTANCE.modifyText(
            OrderedTextUtils.stringVisitableToLegacyString(value)
        );

        if (replaced == null) return value;
        return OrderedTextUtils.legacyStringToStringVisitable(
            replaced
        );
    }
}
