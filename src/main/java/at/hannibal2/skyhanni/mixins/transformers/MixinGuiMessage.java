package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.GuiMessageData;
import at.hannibal2.skyhanni.mixins.hooks.GuiChatHook;
import at.hannibal2.skyhanni.mixins.hooks.MessageIdStore;
import at.hannibal2.skyhanni.utils.ChatUtils;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.MessageSignature;

//? if >= 26.1
import net.minecraft.client.multiplayer.chat.GuiMessageSource;

@Mixin(GuiMessage.class)
public abstract class MixinGuiMessage implements GuiMessageData, MessageIdStore {

    @Unique
    private int skyhanni$messageId;

    @Unique
    @Override
    public int skyhanni$getMessageId() {
        return skyhanni$messageId;
    }

    //? if < 26.1 {
    /*@Unique
    @Override
    public void skyhanni$setMessageId(int id) {
        throw new UnsupportedOperationException("setMessageId is not supported on GuiMessage");
    }
    *///?}

    @Unique
    private Component skyhanni$fullComponent;

    @Unique
    @NotNull
    @Override
    public Component skyhanni$getFullComponent() {
        return skyhanni$fullComponent;
    }

    @Unique
    @Override
    public void skyhanni$setFullComponent(@NotNull Component fullComponent) {
        skyhanni$fullComponent = fullComponent;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(
        int addedTime,
        Component content,
        MessageSignature signature,
        //? if >= 26.1
        GuiMessageSource source,
        GuiMessageTag tag,
        CallbackInfo ci
    ) {
        skyhanni$messageId = ChatUtils.getUniqueGuiMessageId();

        Component component = GuiChatHook.getCurrentComponent();
        skyhanni$fullComponent = component == null ? content : component;
    }
}
