package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(KeyBinding.class)
public interface AccessorKeyBinding {
    @Accessor("pressed")
    boolean getPressed_skyhanni();

    @Accessor("pressed")
    void setPressed_skyhanni(boolean newVal);

    @Accessor("pressTime")
    int getPressTime_skyhanni();

    @Accessor("pressTime")
    void setPressTime_skyhanni(int pressTime);

    @Invoker("unpressKey")
    void skyhanni_unpressKey();
}
