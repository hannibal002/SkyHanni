package at.hannibal2.skyhanni.tweaker;

import org.spongepowered.asm.launch.MixinTweaker;

@SuppressWarnings("unused")
public class SkyHanniTweaker extends MixinTweaker {

    public SkyHanniTweaker() {
        super();

        DownloadSourceChecker.init();
    }

}
