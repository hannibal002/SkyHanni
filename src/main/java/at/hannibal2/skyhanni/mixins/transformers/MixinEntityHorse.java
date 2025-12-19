package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//from neu
@Mixin(value = Horse.class)
public class MixinEntityHorse {
	@Redirect(method = "updateHorseSlots", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/Level;isRemote:Z"), expect = 0)
	public boolean onUpdateHorseSlots(Level instance) {
		if (instance == null)
			return true;
		return instance.isRemote;
	}
}
