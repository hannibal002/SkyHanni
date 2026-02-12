package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.EntityData;
import at.hannibal2.skyhanni.utils.SkyBlockUtils;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import at.hannibal2.skyhanni.mixins.hooks.RendererLivingEntityHook;
import at.hannibal2.skyhanni.utils.StringUtils;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
public class MixinPlayerEntityRenderer {

    @ModifyArg(method = "submitNameTag(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitNameTag(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZIDLnet/minecraft/client/renderer/state/CameraRenderState;)V", ordinal = 0), index = 3)
    private Component modifyRenderLabelIfPresentArgs(Component text) {
        if (SkyBlockUtils.INSTANCE.getInSkyBlock()) {
            return EntityData.getHealthDisplay(text);
        }
        return text;
    }

    @Inject(method = "isEntityUpsideDown(Lnet/minecraft/world/entity/Avatar;)Z", at = @At("HEAD"), cancellable = true)
    private void shouldFlipUpsideDown(Avatar entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Player || entity.hasCustomName()) {
            if (RendererLivingEntityHook.shouldBeUpsideDown(StringUtils.INSTANCE.removeColor(entity.getName().getString(), false))) {
                cir.setReturnValue(true);
            }
        }
    }
}
