package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.EntityData;
import net.minecraft.entity.Entity;
import net.minecraft.util.ChatComponentText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @ModifyVariable(
        method = "getDisplayName",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ChatStyle;setInsertion(Ljava/lang/String;)Lnet/minecraft/util/ChatStyle;", shift = At.Shift.AFTER)
    )
    public ChatComponentText getDisplayName(ChatComponentText value) {
        return EntityData.getDisplayName((Entity) (Object) this, value);
    }

    //from neu
    // Fixes an issue in vanilla code when working with null worlds
    @Inject(method = "getBrightnessForRender", at = @At("HEAD"), cancellable = true)
    public void onGetBrightnessForRender(float p_getBrightnessForRender_1_, CallbackInfoReturnable<Integer> cir) {
        if (((Entity) (Object) this).worldObj == null)
            cir.setReturnValue(-1);
    }

    @Inject(method = "getBrightness", at = @At("HEAD"), cancellable = true)
    public void onGetBrightness(float p_getBrightness_1_, CallbackInfoReturnable<Float> cir) {
        if (((Entity) (Object) this).worldObj == null)
            cir.setReturnValue(1.0F);
    }
}
