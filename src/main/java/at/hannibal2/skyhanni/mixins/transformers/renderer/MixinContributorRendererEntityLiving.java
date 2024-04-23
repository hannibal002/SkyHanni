package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.mixins.hooks.RendererLivingEntityHook;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = RendererLivingEntity.class, priority = 500)
public class MixinContributorRendererEntityLiving<T extends EntityLivingBase> {

    @Unique
    private final RendererLivingEntityHook skyHanni$hook = new RendererLivingEntityHook();

    @Redirect(method = "rotateCorpse", at = @At(value = "INVOKE", target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z", ordinal = 0))
    private boolean rotateCorpse(String displayName, Object v2, T bat, float p_77043_2_, float p_77043_3_, float partialTicks) {
        if (skyHanni$hook.isCoolPerson(displayName)) {
            GlStateManager.scale(1.1f, 1.1f, 1.1f);
            GlStateManager.rotate(skyHanni$hook.getRotation(bat), 0f, 1f, 0f);
        }
        return skyHanni$hook.isCoolPerson(displayName);
    }

    @Redirect(method = "rotateCorpse", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;isWearing(Lnet/minecraft/entity/player/EnumPlayerModelParts;)Z"))
    private boolean rotateCorpse(EntityPlayer bat, EnumPlayerModelParts p_175148_1_) {
        return skyHanni$hook.isWearing(bat, EnumPlayerModelParts.CAPE);
    }
}
