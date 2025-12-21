package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.entity.EntityEquipmentChangeEvent;
import at.hannibal2.skyhanni.features.misc.CurrentPing;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = ClientPacketListener.class)
public class MixinClientPlayNetworkHandler {

    @Inject(method = "handleSetEquipment", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundSetEquipmentPacket;getSlots()Ljava/util/List;", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    public void onEntityEquipmentUpdate(ClientboundSetEquipmentPacket packet, CallbackInfo ci, Entity entity, LivingEntity livingEntity) {
        packet.getSlots().forEach((equipment) -> {
            new EntityEquipmentChangeEvent(entity, equipment.getFirst().getId(), equipment.getSecond()).post();
        });
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/DebugScreenOverlay;showNetworkCharts()Z"))
    public boolean shouldShowPacketSizeAndPingCharts(boolean original) {
        if (!CurrentPing.INSTANCE.isEnabled()) return original;
        return true;
    }
}
