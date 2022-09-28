package at.hannibal2.skyhanni.mixins.transformers

import at.hannibal2.skyhanni.events.PacketEvent
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayClient
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(value = [NetHandlerPlayClient::class], priority = 1001)
abstract class MixinNetHandlerPlayClient : INetHandlerPlayClient {

    //TODO delete this?
    @Shadow
    private val clientWorldController: WorldClient? = null

    @Inject(method = ["addToSendQueue"], at = [At("HEAD")], cancellable = true)
    private fun onSendPacket(packet: Packet<*>, ci: CallbackInfo) {
        if (PacketEvent.SendEvent(packet).postAndCatch()) ci.cancel()
    }
}