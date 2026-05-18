package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.ItemInHandChangeEvent;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
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
    public void setItem(int slot, ItemStack newStack, Operation<Void> original) {
        ItemStack oldStack = getItem(slot);

        original.call(slot, newStack);

        if (slot == getSelectedSlot() && newStack != skyhanni$lastHeldStack) {
            skyhanni$lastHeldSlot = slot;
            skyhanni$lastHeldStack = newStack;
            new ItemInHandChangeEvent(slot, oldStack, slot, newStack).post();
        }
    }

    @WrapMethod(method = "setSelectedSlot")
    public void setSelectedSlot(int newSlot, Operation<Void> original) {
        int oldSlot = getSelectedSlot();
        ItemStack oldStack = getItem(oldSlot);

        original.call(newSlot);

        if (newSlot != skyhanni$lastHeldSlot) {
            ItemStack newStack = getItem(newSlot);
            skyhanni$lastHeldSlot = newSlot;
            skyhanni$lastHeldStack = newStack;
            new ItemInHandChangeEvent(oldSlot, oldStack, newSlot, newStack).post();
        }
    }
}
