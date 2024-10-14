package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//from neu
@Mixin(value = EntityHorse.class, priority = 999)
public class MixinEntityHorse {
	@Redirect(method = "updateHorseSlots", at = @At(value = "FIELD", target = "Lnet/minecraft/world/World;isRemote:Z"))
	public boolean onUpdateHorseSlots(World instance) {
		if (instance == null)
			return true;
		return instance.isRemote;
	}
}
