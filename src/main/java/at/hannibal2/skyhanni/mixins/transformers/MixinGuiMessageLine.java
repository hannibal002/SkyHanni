package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.MessageIdStore;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

//? if >= 26.1 {
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
//?}

@Mixin(GuiMessage.Line.class)
public abstract class MixinGuiMessageLine implements MessageIdStore {

    //? if >= 26.1 {
    @Shadow
    @Final
    private GuiMessage parent;
    //?} else {
    /*@Unique
    private int skyhanni$messageId;
    *///?}

    @Unique
    @Override
    public int skyhanni$getMessageId() {
        //~ if < 26.1 'parent.skyhanni$getMessageId()' -> 'skyhanni$messageId'
        return parent.skyhanni$getMessageId();
    }

    //? if < 26.1 {
    /*@Unique
    @Override
    public void skyhanni$setMessageId(int id) {
        skyhanni$messageId = id;
    }
    *///?}
}
