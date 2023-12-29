package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraftforge.fml.common.eventhandler.EventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EventBus.class)
public interface AccessorEventBus {

    @Accessor("busID")
    int getBusId();
}
