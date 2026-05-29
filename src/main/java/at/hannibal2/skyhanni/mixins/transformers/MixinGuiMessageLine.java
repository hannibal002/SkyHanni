package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.MessageIdStore;
import net.minecraft.client.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GuiMessage.Line.class)
public abstract class MixinGuiMessageLine implements MessageIdStore {

    @Unique
    private int skyhanni$messageId;

    @Unique
    @Override
    public int skyhanni$getMessageId() {
        return skyhanni$messageId;
    }

    @Unique
    @Override
    public void skyhanni$setMessageId(int id) {
        skyhanni$messageId = id;
    }
}
