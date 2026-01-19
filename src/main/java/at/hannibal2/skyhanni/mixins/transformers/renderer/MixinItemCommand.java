package at.hannibal2.skyhanni.mixins.transformers.renderer;
//? > 1.21.9 {
/*import at.hannibal2.skyhanni.mixins.hooks.GlowingStateStore;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SubmitNodeStorage.ItemSubmit.class)
public class MixinItemCommand implements GlowingStateStore {

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
*///?}
