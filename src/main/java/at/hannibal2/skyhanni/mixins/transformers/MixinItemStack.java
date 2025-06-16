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
    //#else
    //$$ @Inject(method = "appendComponentTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/tooltip/TooltipAppender;appendTooltip(Lnet/minecraft/item/Item$TooltipContext;Ljava/util/function/Consumer;Lnet/minecraft/item/tooltip/TooltipType;Lnet/minecraft/component/ComponentsAccess;)V"), cancellable = true)
    //$$ public <T extends net.minecraft.item.tooltip.TooltipAppender> void blockVanillaEnchants(net.minecraft.component.ComponentType<T> componentType, net.minecraft.item.Item.TooltipContext context, net.minecraft.component.type.TooltipDisplayComponent displayComponent, java.util.function.Consumer<net.minecraft.text.Text> textConsumer, net.minecraft.item.tooltip.TooltipType type, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
    //$$     if (at.hannibal2.skyhanni.SkyHanniMod.feature.getInventory().enchantParsing.hideVanillaEnchants.get() && componentType == net.minecraft.component.DataComponentTypes.ENCHANTMENTS) {
    //$$         ci.cancel();
    //$$     }
    //$$ }
    //#endif
}
