package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.EntityData;
import at.hannibal2.skyhanni.mixins.hooks.RendererLivingEntityHook;
import at.hannibal2.skyhanni.utils.SkyBlockUtils;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
public abstract class MixinAvatarRenderer {

    @Inject(method = "isEntityUpsideDown(Lnet/minecraft/world/entity/Avatar;)Z", at = @At("HEAD"), cancellable = true)
    private void shouldFlipUpsideDown(Avatar entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Player || entity.hasCustomName()) {
            if (RendererLivingEntityHook.shouldBeUpsideDown(entity.getUUID())) {
                cir.setReturnValue(true);
            }
        }
    }
}
