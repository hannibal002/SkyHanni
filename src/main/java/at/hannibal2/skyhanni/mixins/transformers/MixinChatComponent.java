package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.chat.ChatPeek;
import at.hannibal2.skyhanni.features.chroma.ChromaFontManagerKt;
import at.hannibal2.skyhanni.features.misc.visualwords.ModifyVisualWords;
import at.hannibal2.skyhanni.mixins.hooks.GuiChatHook;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
//? if < 26.1 {
/*import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.util.FormattedCharSequence;
*///?}
//? if >= 26.1
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ListIterator;
import net.minecraft.client.gui.Gui;

@Mixin(ChatComponent.class)
public abstract class MixinChatComponent {

    @Shadow
    public static int getHeight(double pct) {
        return 0;
    }

    @Shadow
    @Final
    /*? if >= 26.1 {*/private /*?}*/Minecraft minecraft;

    @WrapOperation(
        method = "deleteMessageOrDelay",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;getGuiTicks()I"),
        require = 0
    )
    private int clearChatHead(Gui instance, Operation<Integer> original) {
        return instance.getGuiTicks() + 90;
    }

    @Redirect(method = "deleteMessageOrDelay", at = @At(value = "INVOKE", target = "Ljava/util/ListIterator;set(Ljava/lang/Object;)V"), require = 0)
    private <E> void clearChatTail(ListIterator<E> instance, E e) {
        instance.remove();
    }

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
        ChatComponent.ChatGraphicsAccess graphics,
        int screenHeight,
        int ticks,
        //~ if < 26.1 'ChatComponent.DisplayMode' -> 'boolean'
        ChatComponent.DisplayMode displayMode,
        Operation<Void> original
    ) {
        ChromaFontManagerKt.setRenderingChat(true);
        ModifyVisualWords.INSTANCE.setChangeWords(false);

        original.call(
            graphics,
            screenHeight,
            ticks,
            displayMode
        );

        ChromaFontManagerKt.setRenderingChat(false);
        ModifyVisualWords.INSTANCE.setChangeWords(true);
    }

    //~ if < 26.1 'addMessage' -> 'addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V'
    @Inject(method = "addMessage", at = @At("HEAD"))
    private void setChatLine(
        Component contents,
        MessageSignature signature,
        //? if >= 26.1
        GuiMessageSource source,
        GuiMessageTag tag,
        CallbackInfo ci
    ) {
        GuiChatHook.setCurrentComponent(contents);
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
        line.skyhanni$setMessageId(message.skyhanni$getMessageId());
        return line;
    }
    *///?}
}
