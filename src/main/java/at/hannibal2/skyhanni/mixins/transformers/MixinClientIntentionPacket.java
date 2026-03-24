package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

//TODO: REMOVE THIS!!!!!!
@Mixin(ClientIntentionPacket.class)
public class MixinClientIntentionPacket {
    @ModifyVariable(
        method = "<init>(ILjava/lang/String;ILnet/minecraft/network/protocol/handshake/ClientIntent;)V",
        index = 1,
        at = @At("HEAD"),
        argsOnly = true
    )
    private static int modifyProtocolVersion(int oldVersion) {
        return 775;
    }
}
