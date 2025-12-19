package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.EntityData;
import at.hannibal2.skyhanni.utils.SkyBlockUtils;
import at.hannibal2.skyhanni.utils.compat.TextCompatKt;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
//#if MC > 1.21.8
//$$ import at.hannibal2.skyhanni.mixins.hooks.RendererLivingEntityHook;
//$$ import at.hannibal2.skyhanni.utils.StringUtils;
//$$ import net.minecraft.entity.PlayerLikeEntity;
//$$ import net.minecraft.entity.player.PlayerEntity;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#endif

@Mixin(PlayerRenderer.class)
public class MixinPlayerEntityRenderer {

    //#if MC < 1.21.9
    @ModifyArg(method = "renderNameTag(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;renderNameTag(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", ordinal = 0), index = 1)
    //#else
    //$$ @ModifyArg(method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitLabel(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/math/Vec3d;ILnet/minecraft/text/Text;ZIDLnet/minecraft/client/render/state/CameraRenderState;)V", ordinal = 0), index = 3)
    //#endif
    private Component modifyRenderLabelIfPresentArgs(Component text) {
        if (SkyBlockUtils.INSTANCE.getInSkyBlock()) {
            return EntityData.getHealthDisplay(text);
        }
        return text;
    }

    //#if MC > 1.21.8
    //$$ @Inject(method = "shouldFlipUpsideDown(Lnet/minecraft/entity/PlayerLikeEntity;)Z", at = @At("HEAD"), cancellable = true)
    //$$ private void shouldFlipUpsideDown(PlayerLikeEntity entity, CallbackInfoReturnable<Boolean> cir) {
    //$$     if (entity instanceof PlayerEntity || entity.hasCustomName()) {
    //$$         if (RendererLivingEntityHook.shouldBeUpsideDown(StringUtils.INSTANCE.removeColor(entity.getName().getString(), false))) {
    //$$             cir.setReturnValue(true);
    //$$         }
    //$$     }
    //$$ }
    //#endif
}
