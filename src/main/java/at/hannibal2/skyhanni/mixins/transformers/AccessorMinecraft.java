package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface AccessorMinecraft {

    @Accessor("renderTickCounter")
    //#if MC < 1.21
    //$$ RenderTickCounter getTimer();
    //#else
    RenderTickCounter.Dynamic getTimer();
    //#endif
}
