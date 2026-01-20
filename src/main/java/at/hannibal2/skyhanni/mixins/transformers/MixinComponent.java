package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.ComponentCreatedStore;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MutableComponent.class)
public abstract class MixinComponent implements ComponentCreatedStore {

    @Unique
    private boolean skyhanni$createdMessage = false;

    @Override
    public void skyhanni$setCreated() {
        this.skyhanni$createdMessage = true;
    }

    @Override
    public boolean skyhanni$didCreate() {
        return this.skyhanni$createdMessage;
    }
}
