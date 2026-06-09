package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.RenderEntityOutlineEvent;
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//~ if < 26.2 'net.minecraft.client.renderer.extract.LevelExtractor' -> 'net.minecraft.client.renderer.LevelRenderer'
import net.minecraft.client.renderer.extract.LevelExtractor;

//~ if < 26.2 'LevelExtractor' -> 'LevelRenderer'
@Mixin(LevelExtractor.class)
public abstract class MixinLevelExtractor {

    @Inject(method = "extractVisibleEntities", at = @At(value = "HEAD"))
    public void resetRealGlowing(CallbackInfo ci) {
        RenderLivingEntityHelper.check();
        RenderEntityOutlineEvent noXrayOutlineEvent =
            new RenderEntityOutlineEvent(RenderEntityOutlineEvent.Type.NO_XRAY, null);
        RenderLivingEntityHelper.setCurrentGlowEvent(noXrayOutlineEvent);
        noXrayOutlineEvent.post();
    }
}
