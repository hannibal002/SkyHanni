package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.chat.ChatPeek;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if < 26.1 {
/*import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.util.FormattedCharSequence;
*///?}

@Mixin(ChatComponent.class)
public abstract class MixinChatComponent {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    public static int getHeight(double heightOption) {
        return 0;
    }

    //? if < 26.1 {
    /*@WrapOperation(
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
        line.skyhanni$setParent(message);
        return line;
    }
    *///?}

    @Inject(method = "getHeight()I", at = @At("HEAD"), cancellable = true)
    private void getHeight(CallbackInfoReturnable<Integer> cir) {
        if (ChatPeek.peek()) {
            cir.setReturnValue(getHeight(this.minecraft.options.chatHeightFocused().get()));
        }
    }

    //? if >= 26.1 {
    @WrapMethod(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V")
    //?} else {
    /*@WrapMethod(method = "render(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IIZ)V")
    *///?}
    private void wrapRender(
        ChatComponent.ChatGraphicsAccess chatGraphicsAccess,
        int screenHeight,
        int ticks,
        //~ if < 26.1 'ChatComponent.DisplayMode' -> 'boolean'
        ChatComponent.DisplayMode displayMode,
        Operation<Void> original
    ) {
    }
}
