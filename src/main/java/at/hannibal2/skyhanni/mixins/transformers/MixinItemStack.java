package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
//? if > 1.21.11
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ItemStack.class)
public class MixinItemStack {

    //? if > 1.21.11 {
    @Shadow private net.minecraft.core.Holder<net.minecraft.world.item.Item> item;

    @Inject(method = "isEmpty", at = @At("HEAD"), cancellable = true)
    private void skyhanni$handleNullHolder(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (this.item == null) {
            cir.setReturnValue(true);
        }
    }
    //? }

    @Inject(method = "addToTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/TooltipProvider;addToTooltip(Lnet/minecraft/world/item/Item$TooltipContext;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;Lnet/minecraft/core/component/DataComponentGetter;)V"), cancellable = true)
    public <T extends net.minecraft.world.item.component.TooltipProvider> void blockVanillaEnchants(net.minecraft.core.component.DataComponentType<T> componentType, net.minecraft.world.item.Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay displayComponent, java.util.function.Consumer<net.minecraft.network.chat.Component> textConsumer, net.minecraft.world.item.TooltipFlag type, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (at.hannibal2.skyhanni.utils.SkyBlockUtils.INSTANCE.getInSkyBlock() && at.hannibal2.skyhanni.SkyHanniMod.feature.getInventory().getEnchantParsing().getHideVanillaEnchants().get() && componentType == net.minecraft.core.component.DataComponents.ENCHANTMENTS) {
            ci.cancel();
        }
    }
}
