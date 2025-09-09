package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.EntityData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerEntity.class)
public class MixinEntityPlayer {

    //#if MC < 1.21
    //$$ @ModifyVariable(
    //$$     method = "getDisplayName",
    //$$     at = @At(value = "INVOKE", target = "Lnet/minecraft/text/Style;setInsertion(Ljava/lang/String;)Lnet/minecraft/text/Style;", shift = At.Shift.AFTER)
    //$$ )
    //$$ public Text getDisplayName(Text value) {
    //$$     return EntityData.getDisplayName((PlayerEntity) (Object) this, (LiteralText) value);
    //$$ }
    //#else
    @org.spongepowered.asm.mixin.injection.Inject(method = "getDisplayName", at = @At(value = "RETURN"), cancellable = true)
    public void getDisplayName(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Text> cir) {
        cir.setReturnValue(
            EntityData.getDisplayName((PlayerEntity) (Object) this, cir.getReturnValue())
        );
    }
    //#endif
}
