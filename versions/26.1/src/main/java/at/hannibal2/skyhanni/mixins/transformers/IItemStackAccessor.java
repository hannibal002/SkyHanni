package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.core.Holder;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStack.class)
public interface IItemStackAccessor {
    @Accessor("item") @Nullable Holder<Item> skyhanni$getItemHolder();
    @Accessor("item") void skyhanni$setItemHolder(@Nullable Holder<Item> item);
    @Accessor("components") PatchedDataComponentMap skyhanni$getPatchedComponents();
    @Accessor("components") void skyhanni$setPatchedComponents(PatchedDataComponentMap components);
}
