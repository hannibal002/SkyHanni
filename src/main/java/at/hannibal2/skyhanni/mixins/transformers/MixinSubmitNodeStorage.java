package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.GlowingStateStore;
import at.hannibal2.skyhanni.mixins.hooks.RenderAlphaStore;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({
    SubmitNodeStorage.ItemSubmit.class,
    SubmitNodeStorage.ModelPartSubmit.class,
    SubmitNodeStorage.ModelSubmit.class,
})
public abstract class MixinSubmitNodeStorage implements GlowingStateStore, RenderAlphaStore {

    @Unique
    private boolean skyhanni$usingCustomOutline = false;

    @Unique
    private int skyhanni$renderAlpha = 255;

    @Override
    public void skyhanni$setRenderAlpha(int alpha) {
        this.skyhanni$renderAlpha = alpha;
    }

    @Override
    public int skyhanni$getRenderAlpha() {
        return this.skyhanni$renderAlpha;
    }

    @Override
    public void skyhanni$setUsingCustomOutline() {
        this.skyhanni$usingCustomOutline = true;
    }

    @Override
    public boolean skyhanni$isUsingCustomOutline() {
        return this.skyhanni$usingCustomOutline;
    }
}
