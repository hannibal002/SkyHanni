package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.SkyHanniMod;
import at.hannibal2.skyhanni.utils.system.PlatformUtils;
import kotlin.Unit;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen {

    @Unique
    private static boolean skyhanni$hasInited = false;

    @Inject(method = "<init>(ZLnet/minecraft/client/gui/components/LogoRenderer;)V", at = @At("RETURN"))
    private void onCreate(boolean doBackgroundFade, LogoRenderer logoRenderer, CallbackInfo ci) {
        if (!skyhanni$hasInited && PlatformUtils.isDevEnvironment()) {
            at.hannibal2.skyhanni.test.SkyHanniDebugsAndTests.loadAllMixinClasses();
        }
        skyhanni$hasInited = true;
    }
}
