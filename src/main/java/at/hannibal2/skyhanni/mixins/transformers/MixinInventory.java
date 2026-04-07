package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.HeldItemChangeEvent;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Inventory.class)
public abstract class MixinInventory {

    @Unique
    private int skyhanni$lastHeldSlot = -1;

    @Unique
    private ItemStack skyhanni$lastHeldStack = ItemStack.EMPTY;

    @Shadow
    public abstract ItemStack getItem(int slot);

    @Shadow
    public abstract int getSelectedSlot();

    @WrapMethod(method = "setItem")
    public void setItem(int slot, ItemStack stack, Operation<Void> original) {
        original.call(slot, stack);
        if (slot == getSelectedSlot() && stack != skyhanni$lastHeldStack) {
            skyhanni$lastHeldSlot = slot;
            skyhanni$lastHeldStack = stack;
            new HeldItemChangeEvent(stack, slot).post();
        }
    }

    @WrapMethod(method = "setSelectedSlot")
    public void setSelectedSlot(int slot, Operation<Void> original) {
        original.call(slot);
        if (slot != skyhanni$lastHeldSlot) {
            ItemStack stack = getItem(slot);
            skyhanni$lastHeldSlot = slot;
            skyhanni$lastHeldStack = stack;
            new HeldItemChangeEvent(getItem(slot), slot).post();
        }
    }
}
