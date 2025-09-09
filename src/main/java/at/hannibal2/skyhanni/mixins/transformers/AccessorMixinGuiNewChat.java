package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ChatHud.class)
public interface AccessorMixinGuiNewChat {

    @Accessor("messages")
    List<ChatHudLine> getChatLines_skyhanni();

    @Accessor("messages")
    void setChatLines_skyhanni(List<ChatHudLine> chatLines);

    @Accessor("visibleMessages")
    List<ChatHudLine> getDrawnChatLines_skyhanni();

    @Accessor("visibleMessages")
    void setDrawnChatLines_skyhanni(List<ChatHudLine> drawnChatLines);

    @Accessor("scrolledLines")
    int getScrollPos_skyhanni();

    @Invoker("reset")
    void refreshChat_skyhanni();
}
