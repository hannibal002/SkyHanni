package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.RenderAlphaStore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

//? if >= 26.2 {
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
//?} else {
/*import net.minecraft.client.renderer.SubmitNodeStorage;
*///?}

@Mixin({
    //? if >= 26.2 {
    ItemFeatureRenderer.Submit.class,
    ModelFeatureRenderer.Submit.class,
    //?} else {
    /*SubmitNodeStorage.ItemSubmit.class,
    SubmitNodeStorage.ModelSubmit.class,
    *///?}
})
public abstract class MixinRenderAlphaStorage implements RenderAlphaStore {

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
}
