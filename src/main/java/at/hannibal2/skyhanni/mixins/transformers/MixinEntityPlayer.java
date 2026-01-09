package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.EntityData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public class MixinEntityPlayer {

    @org.spongepowered.asm.mixin.injection.Inject(method = "getDisplayName", at = @At(value = "RETURN"), cancellable = true)
    public void getDisplayName(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Component> cir) {
        cir.setReturnValue(
            EntityData.getDisplayName((Player) (Object) this, cir.getReturnValue())
        );
    }
}
