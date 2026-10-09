package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.ParticleSuppressionStore;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ClientboundLevelParticlesPacket.class)
public abstract class MixinClientboundLevelParticlesPacket implements ParticleSuppressionStore {
    @Unique
    private boolean skyhanni$shouldSuppress = false;

    @Unique
    @Override
    public boolean skyhanni$shouldSuppress() {
        return skyhanni$shouldSuppress;
    }

    @Unique
    @Override
    public void skyhanni$setShouldSuppress(boolean value) {
        skyhanni$shouldSuppress = value;
    }
}
