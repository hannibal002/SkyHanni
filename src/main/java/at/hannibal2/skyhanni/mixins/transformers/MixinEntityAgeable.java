package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.entity.EntityAgeable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//from neu
@Mixin(EntityAgeable.class)
public class MixinEntityAgeable {
	@Shadow
	protected int growingAge;

	@Inject(method = "getGrowingAge", cancellable = true, at = @At("HEAD"))
	public void onGetGrowingAge(CallbackInfoReturnable<Integer> cir) {
		if (((EntityAgeable) (Object) this).worldObj == null)
			cir.setReturnValue(growingAge);
	}
}
