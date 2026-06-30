package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.SkyHanniMod;
import at.hannibal2.skyhanni.utils.SkyBlockUtils;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TooltipProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.function.Consumer;

//? if >= 26.1 {
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Shadow;
//?}

@Mixin(ItemStack.class)
public class MixinItemStack {

    //? if >= 26.1 {
    @Shadow private Holder<Item> item;

    @Inject(method = "isEmpty", at = @At("HEAD"), cancellable = true)
    private void skyhanni$handleNullHolder(CallbackInfoReturnable<Boolean> cir) {
        if (this.item == null) {
            cir.setReturnValue(true);
        }
    }
    //?}

    @Inject(method = "addToTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/TooltipProvider;addToTooltip(Lnet/minecraft/world/item/Item$TooltipContext;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;Lnet/minecraft/core/component/DataComponentGetter;)V"), cancellable = true)
    public <T extends TooltipProvider> void blockVanillaEnchants(DataComponentType<T> componentType, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type, CallbackInfo ci) {
        if (SkyBlockUtils.getInSkyBlock() && SkyHanniMod.feature.getInventory().getEnchantParsing().getHideVanillaEnchants().get() && componentType == DataComponents.ENCHANTMENTS) {
            ci.cancel();
        }
    }
}
