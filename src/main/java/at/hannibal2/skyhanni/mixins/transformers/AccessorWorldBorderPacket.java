package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.network.packet.s2c.play.WorldBorderS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldBorderS2CPacket.class)
public interface AccessorWorldBorderPacket {
    @Accessor("action")
    WorldBorderS2CPacket.Action getAction();

    @Accessor("warningTime")
    int getWarningTime();
}
