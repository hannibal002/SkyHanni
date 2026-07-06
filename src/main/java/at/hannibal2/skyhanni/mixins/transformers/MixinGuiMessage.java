package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.GuiChatHook;
import at.hannibal2.skyhanni.mixins.hooks.GuiMessageData;
import at.hannibal2.skyhanni.utils.ChatUtils;
import net.minecraft.client.multiplayer.chat.GuiMessage;
//? if >= 26.1
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMessage.class)
public abstract class MixinGuiMessage implements GuiMessageData {

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
        Component component = GuiChatHook.getCurrentComponent();
        // Clear current component for compatibility with mods that inject messages into the chat
        // history, such as Chat Patches' persistent history feature
        GuiChatHook.setCurrentComponent(null);
        skyhanni$fullComponent = component == null ? content : component;
    }
}
