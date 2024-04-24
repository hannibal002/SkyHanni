package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.mixins.hooks.RendererLivingEntityHook;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "codes/biscuit/skyblockaddons/asm/hooks/RendererLivingEntityHook")
public class MixinRendererLivingEntityHookSBA<T extends EntityLivingBase> {

    @Unique
    private static final RendererLivingEntityHook skyHanni$hook = new RendererLivingEntityHook();

    @Inject(method = "equals", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onEquals(String displayName, Object otherString, CallbackInfoReturnable<Boolean> cir) {
        skyHanni$hook.onEquals(displayName, cir);
    }

    @Inject(method = "isWearing", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onIsWearing(EntityPlayer entityPlayer, EnumPlayerModelParts p_175148_1_, CallbackInfoReturnable<Boolean> cir) {
        skyHanni$hook.onIsWearing(entityPlayer, cir);
    }
}
