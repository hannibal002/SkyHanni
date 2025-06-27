package at.hannibal2.skyhanni.mixins.transformers.gui;

import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SimpleInventory.class)
public class MixinInventoryBasic {

    @Shadow
    @Final
    public DefaultedList<ItemStack> heldStacks;

    @Inject(method = "getStack", at = @At("HEAD"), cancellable = true)
    public void getStack(int slot, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack[] stacks = this.heldStacks.toArray(new ItemStack[0]);

        ReplaceItemEvent.postEvent((SimpleInventory) (Object) this, stacks, slot, cir);
    }
}
