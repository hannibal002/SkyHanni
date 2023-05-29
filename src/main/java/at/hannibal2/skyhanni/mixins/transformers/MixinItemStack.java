package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.ItemStackCachedData;
import at.hannibal2.skyhanni.utils.CachedItemData;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemStack.class)
public class MixinItemStack implements ItemStackCachedData {

    public CachedItemData skyhanni_cachedData = new CachedItemData();

    public CachedItemData getSkyhanni_cachedData() {
        return skyhanni_cachedData;
    }
}
