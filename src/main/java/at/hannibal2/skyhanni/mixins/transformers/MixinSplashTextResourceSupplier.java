package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.utils.LorenzColor;
import at.hannibal2.skyhanni.utils.chat.TextHelper;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Random;

@Mixin(SplashManager.class)
public class MixinSplashTextResourceSupplier {

    //? if < 1.21.11 {
    @Shadow
    @Final
    private List<String> splashes;

    @Inject(method = "apply(Ljava/util/List;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("TAIL"))
    public void addSplash(List<String> list, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci) {
        this.splashes.add("SkyHanni!");
    }
    //?} else {

    /*@Shadow
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
    *///?}
}
