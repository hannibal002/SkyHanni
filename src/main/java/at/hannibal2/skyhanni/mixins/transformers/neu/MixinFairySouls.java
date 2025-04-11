package at.hannibal2.skyhanni.mixins.transformers.neu;

import at.hannibal2.skyhanni.features.misc.NeuSoulPathFind;
import io.github.moulberry.notenoughupdates.miscfeatures.FairySouls;
import net.minecraft.util.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;

@Pseudo
@Mixin(value = FairySouls.class, remap = false)
public class MixinFairySouls {

    @Shadow
    private List<BlockPos> closestMissingSouls;

    @Shadow
    private Set<Integer> foundSoulsInLocation;

    @Shadow
    private List<BlockPos> allSoulsInCurrentLocation;

    @Shadow
    private TreeMap<Double, BlockPos> missingSoulsDistanceSqMap;

    @Shadow
    private boolean showSouls;

    @Shadow
    private boolean trackSouls;

    @Shadow
    private String currentLocation;

    @Inject(method = "onRenderLast", at = @At(value = "TAIL"))
    public void onRenderLast_skyhanni(CallbackInfo ci) {
        if (!showSouls || !trackSouls || currentLocation == null || closestMissingSouls.isEmpty()) {
            return;
        }
        NeuSoulPathFind.render();
    }

    @Inject(method = "refreshMissingSoulInfo", at = @At(value = "TAIL"))
    public void refreshMissingSoulInfo_skyhanni(CallbackInfo ci) {
        NeuSoulPathFind.updateList(allSoulsInCurrentLocation, missingSoulsDistanceSqMap);
    }
}
