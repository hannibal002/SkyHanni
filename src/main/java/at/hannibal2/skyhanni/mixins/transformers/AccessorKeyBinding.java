package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyBinding.class)
public interface AccessorKeyBinding {

    @Accessor("pressTime")
    int getPressTime_skyhanni();

    @Accessor("pressTime")
    void setPressTime_skyhanni(int pressTime);
}
