package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.entity.EntityEquipmentChangeEvent;
import at.hannibal2.skyhanni.events.minecraft.packet.PacketSentEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = ClientPacketListener.class, priority = 1001)
public abstract class MixinNetHandlerPlayClient implements INetHandlerPlayClient {

    @Inject(method = "addToSendQueue", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        ClientPacketListener handler = (ClientPacketListener) (Object) this;
        if (new PacketSentEvent(packet).post()) {
            ci.cancel();
        }
    }

    @Inject(method = "handleEntityEquipment", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setCurrentItemOrArmor(ILnet/minecraft/world/item/ItemStack;)V", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    public void onEntityEquipment(ClientboundSetEquipmentPacket packetIn, CallbackInfo ci, Entity entity) {
        new EntityEquipmentChangeEvent(entity, packetIn.getEquipmentSlot(), packetIn.getItemStack()).post();
    }

}
