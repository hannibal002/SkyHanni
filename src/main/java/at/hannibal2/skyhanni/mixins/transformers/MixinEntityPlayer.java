package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.EntityData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntityPlayer.class)
public class MixinEntityPlayer {

    //#if MC < 1.21
    @ModifyVariable(
        method = "getDisplayName",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ChatStyle;setInsertion(Ljava/lang/String;)Lnet/minecraft/util/ChatStyle;", shift = At.Shift.AFTER)
    )
    public IChatComponent getDisplayName(IChatComponent value) {
        return EntityData.getDisplayName((EntityPlayer) (Object) this, (ChatComponentText) value);
    }
    //#else
    //$$ @org.spongepowered.asm.mixin.injection.Inject(method = "getDisplayName", at = @At(value = "RETURN"), cancellable = true)
    //$$ public void getDisplayName(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Text> cir) {
    //$$     cir.setReturnValue(
    //$$         EntityData.getDisplayName((PlayerEntity) (Object) this, cir.getReturnValue())
    //$$     );
    //$$ }
    //#endif
}
