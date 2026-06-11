package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.utils.system.PlatformUtils;
import net.minecraft.SharedConstants;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientIntentionPacket.class)
public abstract class MixinClientIntentionPacket {

    @ModifyVariable(
        method = "<init>(ILjava/lang/String;ILnet/minecraft/network/protocol/handshake/ClientIntent;)V",
        at = @At("HEAD"),
        argsOnly = true,
        name = "protocolVersion"
    )
    private static int modifyProtocolVersion(int protocolVersion) {
        if (!PlatformUtils.isDevEnvironment()) return protocolVersion;
        if (!Boolean.getBoolean("skyhanni.snapshotProtocolDebug")) return protocolVersion;
        //noinspection deprecation
        return SharedConstants.RELEASE_NETWORK_PROTOCOL_VERSION;
    }
}
