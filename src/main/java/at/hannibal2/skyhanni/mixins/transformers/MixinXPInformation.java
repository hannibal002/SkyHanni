package at.hannibal2.skyhanni.mixins.transformers;

import io.github.moulberry.notenoughupdates.util.XPInformation;
import io.github.moulberry.notenoughupdates.util.XPInformation.SkillInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "io.github.moulberry.notenoughupdates.util.XPInformation.SkillInfo")
public interface MixinXPInformation {

    @Accessor(value = "totalXp", remap = false)
    double getTotalXp();

}
