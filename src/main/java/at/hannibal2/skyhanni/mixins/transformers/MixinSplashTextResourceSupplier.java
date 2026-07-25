package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.utils.LorenzColor;
import at.hannibal2.skyhanni.utils.chat.TextHelper;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(SplashManager.class)
public class MixinSplashTextResourceSupplier {

    @Shadow
    private List<Component> splashes;

    @Shadow
    @Final
    private static RandomSource RANDOM;

    @Inject(method = "getSplash", at = @At("HEAD"), cancellable = true)
    public void addSkyhanniSplash(CallbackInfoReturnable<SplashRenderer> cir) {
        if (RANDOM.nextInt(this.splashes.size() + 1) == this.splashes.size()) {
            cir.setReturnValue(new SplashRenderer(TextHelper.INSTANCE.createGradientText(LorenzColor.YELLOW, LorenzColor.GOLD, "SkyHanni!")));
        }
    }
}
