package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.MessageIdStore;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GuiMessage.Line.class)
public abstract class MixinGuiMessageLine implements MessageIdStore {

    @Shadow
    @Final
    private GuiMessage parent;

    @Unique
    @Override
    public int skyhanni$getMessageId() {
        return parent.skyhanni$getMessageId();
    }
}
