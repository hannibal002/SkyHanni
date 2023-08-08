package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.ItemStackCachedData;
import at.hannibal2.skyhanni.utils.CachedItemData;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemStack.class)
public class MixinItemStack implements ItemStackCachedData {

    @Unique
    public CachedItemData skyhanni_cachedData = new CachedItemData();

    public CachedItemData getSkyhanni_cachedData() {
        return skyhanni_cachedData;
    }
}
