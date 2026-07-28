package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.GlowingStateStore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

//? if >= 26.2 {
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
//?} else {
/*import net.minecraft.client.renderer.SubmitNodeStorage;
*///?}

@Mixin({
    ItemFeatureRenderer.Submit.class,
    ModelFeatureRenderer.Submit.class,
    //? if < 26.2 {
    /*SubmitNodeStorage.ModelPartSubmit.class,
    *///?}
})
public class MixinSubmitNodeStorage implements GlowingStateStore {

    @Unique
    private boolean skyhanni$usingCustomOutline = false;

    @Override
    public boolean skyhanni$isUsingCustomOutline() {
        return this.skyhanni$usingCustomOutline;
    }

    @Override
    public void skyhanni$setUsingCustomOutline() {
        this.skyhanni$usingCustomOutline = true;
    }
}
