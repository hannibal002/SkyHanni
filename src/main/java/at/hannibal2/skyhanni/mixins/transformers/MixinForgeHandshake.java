package at.hannibal2.skyhanni.mixins.transformers;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.network.handshake.FMLHandshakeMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(FMLHandshakeMessage.ModList.class)
public class MixinForgeHandshake {

    @Shadow(
        remap = false
    )
    private Map<String, String> modTags;

    @Inject(
        at = @At("HEAD"),
        method = "toBytes",
        remap = false
    )
    public void onToBytes(ByteBuf buffer, CallbackInfo ci) {
        for (ModContainer mod : Loader.instance().getActiveModList()) {
            this.modTags.put(mod.getModId(), mod.getVersion());
        }
    }
}
