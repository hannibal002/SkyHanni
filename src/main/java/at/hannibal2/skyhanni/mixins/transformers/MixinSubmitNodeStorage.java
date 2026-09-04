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
    //? if >= 26.2 {
    ItemFeatureRenderer.Submit.class,
    ModelFeatureRenderer.Submit.class,
    //?} else {
    /*SubmitNodeStorage.ItemSubmit.class,
    SubmitNodeStorage.ModelPartSubmit.class,
    SubmitNodeStorage.ModelSubmit.class,
    *///?}
})
public abstract class MixinSubmitNodeStorage implements GlowingStateStore {

    @Unique
    private boolean skyhanni$usingCustomOutline = false;

    @Override
    public void skyhanni$setUsingCustomOutline() {
        this.skyhanni$usingCustomOutline = true;
    }

    @Override
    public boolean skyhanni$isUsingCustomOutline() {
        return this.skyhanni$usingCustomOutline;
    }
}
