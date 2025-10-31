package at.hannibal2.hanni.mixins.transformers;

import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(GuiNewChat.class)
public interface AccessorMixinGuiNewChat {

    @Accessor("chatLines")
    List<ChatLine> getChatLines_hanni();

    @Accessor("chatLines")
    void setChatLines_hanni(List<ChatLine> chatLines);

    @Accessor("drawnChatLines")
    List<ChatLine> getDrawnChatLines_hanni();

    @Accessor("drawnChatLines")
    void setDrawnChatLines_hanni(List<ChatLine> drawnChatLines);

    @Accessor("scrollPos")
    int getScrollPos_hanni();

    @Invoker("refreshChat")
    void refreshChat_hanni();
}
