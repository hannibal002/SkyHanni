package at.hannibal2.skyhanni.mixins.transformers.gui;

import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SimpleContainer.class)
public class MixinInventoryBasic {

    @ModifyExpressionValue(method = "getItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;get(I)Ljava/lang/Object;"))
    public Object getStack(Object original, @Local(argsOnly = true) int slot) {
        return ReplaceItemEvent.postEvent((SimpleContainer) (Object) this, (ItemStack) original, slot);
    }
}
