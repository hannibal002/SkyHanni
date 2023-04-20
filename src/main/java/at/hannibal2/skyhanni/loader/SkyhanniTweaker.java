package at.hannibal2.skyhanni.loader;

import at.hannibal2.skyhanni.utils.LorenzUtils;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

import java.io.File;
import java.util.List;

public class SkyhanniTweaker implements ITweaker {
    static {
        LorenzUtils.checkIfNeuIsLoaded();
//        EnvironmentScan.checkEnvironmentOnce();
    }

//    List<String> delegates = new ArrayList<>();

    public SkyhanniTweaker() {
//        discoverTweakers();
//        System.out.println("SkyHanni Delegating Tweaker is loaded with: " + delegates);
    }

//    private void discoverTweakers() {
//        delegates.add(KotlinLoadingTweaker.class.getName());
//    }

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
//        List<String> tweakClasses = (List<String>) Launch.blackboard.get("TweakClasses");
//        tweakClasses.addAll(delegates);
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
    }

    @Override
    public String getLaunchTarget() {
        return null;
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[0];
    }

}
