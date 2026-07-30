package at.hannibal2.skyhanni.mixins.transformers;

//? if < 26.1 {
/*import at.hannibal2.skyhanni.mixins.hooks.MessageStore;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GuiMessage.Line.class)
public abstract class MixinGuiMessage$Line implements MessageStore {

    @Unique
    private GuiMessage skyhanni$parent;

    @Unique
    @Override
    public GuiMessage skyhanni$getParent() {
        return skyhanni$parent;
    }

    @Unique
    @Override
    public void skyhanni$setParent(GuiMessage parent) {
        skyhanni$parent = parent;
    }
}
*///?}
