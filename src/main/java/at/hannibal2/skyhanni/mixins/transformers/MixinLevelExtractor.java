package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public abstract class MixinLevelExtractor {
    @Inject(method = "extractVisibleEntities", at = @At(value = "HEAD"))
    public void resetRealGlowing(CallbackInfo ci) {
        RenderLivingEntityHelper.postNoXrayOutlineEvent();
    }
}
