package at.hannibal2.skyhanni.mixins.transformers;

//#if MC < 1.21
import at.hannibal2.skyhanni.data.ToolTipData;
//#endif
import at.hannibal2.skyhanni.mixins.hooks.ItemStackCachedData;
import at.hannibal2.skyhanni.utils.CachedItemData;
import at.hannibal2.skyhanni.utils.compat.DrawContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(ItemStack.class)
public class MixinItemStack implements ItemStackCachedData {

    @Unique
    public CachedItemData skyhanni_cachedData = new CachedItemData((Void) null);

    public CachedItemData getSkyhanni_cachedData() {
        return skyhanni_cachedData;
    }

    //#if MC < 1.21
    @Inject(method = "getTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/event/ForgeEventFactory;onItemTooltip(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/EntityPlayer;Ljava/util/List;Z)Lnet/minecraftforge/event/entity/player/ItemTooltipEvent;", shift = At.Shift.BEFORE, remap = false), locals = LocalCapture.CAPTURE_FAILHARD)
    public void getTooltip(EntityPlayer playerIn, boolean advanced, CallbackInfoReturnable<List<String>> cir, List<String> list) {
        ItemStack stack = (ItemStack) (Object) this;
        ToolTipData.onHover(new DrawContext(), stack, list);
    }
    //#endif
}
