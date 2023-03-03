package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.client.settings.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(KeyBinding.class)
public interface AccessorKeyBinding {

    @Invoker("unpressKey")
     void skyhanni_unpressKey();
}
