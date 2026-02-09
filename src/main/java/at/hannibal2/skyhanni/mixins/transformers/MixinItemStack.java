package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(ItemStack.class)
public class MixinItemStack {

    @Inject(method = "addToTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/TooltipProvider;addToTooltip(Lnet/minecraft/world/item/Item$TooltipContext;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;Lnet/minecraft/core/component/DataComponentGetter;)V"), cancellable = true)
    public <T extends net.minecraft.world.item.component.TooltipProvider> void blockVanillaEnchants(net.minecraft.core.component.DataComponentType<T> componentType, net.minecraft.world.item.Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay displayComponent, java.util.function.Consumer<net.minecraft.network.chat.Component> textConsumer, net.minecraft.world.item.TooltipFlag type, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (at.hannibal2.skyhanni.utils.SkyBlockUtils.INSTANCE.getInSkyBlock() && at.hannibal2.skyhanni.SkyHanniMod.feature.getInventory().getEnchantParsing().getHideVanillaEnchants().get() && componentType == net.minecraft.core.component.DataComponents.ENCHANTMENTS) {
            ci.cancel();
        }
    }
}
