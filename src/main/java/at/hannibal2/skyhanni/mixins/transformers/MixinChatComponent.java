package at.hannibal2.skyhanni.mixins.transformers;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChatComponent.class)
public abstract class MixinChatComponent {

    @WrapOperation(
        method = "addMessageToDisplayQueue",
        at = @At(
            value = "NEW",
            target = "net/minecraft/client/GuiMessage$Line"
        )
    )
    private GuiMessage.Line addMessageId(
        int addedTime,
        FormattedCharSequence content,
        GuiMessageTag tag,
        boolean endOfEntry,
        Operation<GuiMessage.Line> original,
        GuiMessage message
    ) {
        GuiMessage.Line line = original.call(addedTime, content, tag, endOfEntry);
        line.skyhanni$setMessageId(message.skyhanni$getMessageId());
        return line;
    }
}
