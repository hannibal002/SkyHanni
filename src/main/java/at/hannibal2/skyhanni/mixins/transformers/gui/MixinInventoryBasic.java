package at.hannibal2.skyhanni.mixins.transformers.gui;

import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SimpleContainer.class)
public class MixinInventoryBasic {

    @Shadow
    @Final
    public NonNullList<ItemStack> items;

    @Inject(method = "getItem", at = @At("HEAD"), cancellable = true)
    public void getStack(int slot, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack[] stacks = this.items.toArray(new ItemStack[0]);

        ReplaceItemEvent.postEvent((SimpleContainer) (Object) this, stacks, slot, cir);
    }
}
