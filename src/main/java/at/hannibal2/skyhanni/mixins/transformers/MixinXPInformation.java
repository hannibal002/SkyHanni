package at.hannibal2.skyhanni.mixins.transformers;

import io.github.moulberry.notenoughupdates.util.XPInformation;
import io.github.moulberry.notenoughupdates.util.XPInformation.SkillInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(SkillInfo.class)
public class MixinXPInformation extends XPInformation.SkillInfo {

    @Shadow
    public double totalXp;

    @Unique
    public double getTotalXp() {
        return this.totalXp;
    }
}
