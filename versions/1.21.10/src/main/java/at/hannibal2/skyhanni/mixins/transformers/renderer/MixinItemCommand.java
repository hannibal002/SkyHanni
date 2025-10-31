package at.hannibal2.hanni.mixins.transformers.renderer;

import at.hannibal2.hanni.mixins.hooks.GlowingStateStore;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(OrderedRenderCommandQueueImpl.ItemCommand.class)
public class MixinItemCommand implements GlowingStateStore {

    @Unique
    private boolean hanni$usingCustomOutline = false;

    @Override
    public void hanni$setUsingCustomOutline() {
        this.hanni$usingCustomOutline = true;
    }

    @Override
    public boolean hanni$isUsingCustomOutline() {
        return this.hanni$usingCustomOutline;
    }

}
