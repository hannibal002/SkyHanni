package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface AccessorMinecraft {

    @Accessor("deltaTracker")
    DeltaTracker.Timer getTimer();
}
