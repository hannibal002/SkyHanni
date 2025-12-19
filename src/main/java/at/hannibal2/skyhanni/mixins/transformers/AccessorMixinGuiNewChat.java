package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ChatComponent.class)
public interface AccessorMixinGuiNewChat {

    @Accessor("allMessages")
    List<GuiMessage> getChatLines_skyhanni();

    @Accessor("allMessages")
    void setChatLines_skyhanni(List<GuiMessage> chatLines);

    @Accessor("trimmedMessages")
    List<GuiMessage> getDrawnChatLines_skyhanni();

    @Accessor("trimmedMessages")
    void setDrawnChatLines_skyhanni(List<GuiMessage> drawnChatLines);

    @Accessor("chatScrollbarPos")
    int getScrollPos_skyhanni();

    @Invoker("rescaleChat")
    void refreshChat_skyhanni();
}
