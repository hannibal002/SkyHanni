package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.ToolTipData;
import at.hannibal2.skyhanni.mixins.hooks.ItemStackCachedData;
import at.hannibal2.skyhanni.utils.CachedItemData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public class MixinItemStack implements ItemStackCachedData {

    @Unique
    public CachedItemData skyhanni_cachedData = new CachedItemData((Void) null);

    public CachedItemData getSkyhanni_cachedData() {
        return skyhanni_cachedData;
    }

    @Inject(method = "getTooltip", at = @At("RETURN"))
    public void getTooltip(EntityPlayer playerIn, boolean advanced, CallbackInfoReturnable<List<String>> ci) {
        ItemStack stack = (ItemStack) (Object) this;
        ToolTipData.onHover(stack, ci.getReturnValue());
    }
}
