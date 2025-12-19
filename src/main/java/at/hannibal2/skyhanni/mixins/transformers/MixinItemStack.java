package at.hannibal2.skyhanni.mixins.transformers;

//#if MC < 1.21
//$$ import at.hannibal2.skyhanni.data.ToolTipData;
//#endif
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(ItemStack.class)
public class MixinItemStack {

    //#if MC < 1.21
    //$$ @Inject(method = "getTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/event/ForgeEventFactory;onItemTooltip(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/PlayerEntity;Ljava/util/List;Z)Lnet/minecraftforge/event/entity/player/ItemTooltipEvent;", shift = At.Shift.BEFORE, remap = false), locals = LocalCapture.CAPTURE_FAILHARD)
    //$$ public void getTooltip(PlayerEntity playerIn, boolean advanced, CallbackInfoReturnable<List<String>> cir, List<String> list) {
    //$$     ItemStack stack = (ItemStack) (Object) this;
    //$$     ToolTipData.onHover(new DrawContext(), stack, list);
    //$$ }
    //#else
    @Inject(method = "addToTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/TooltipProvider;addToTooltip(Lnet/minecraft/world/item/Item$TooltipContext;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;Lnet/minecraft/core/component/DataComponentGetter;)V"), cancellable = true)
    public <T extends net.minecraft.world.item.component.TooltipProvider> void blockVanillaEnchants(net.minecraft.core.component.DataComponentType<T> componentType, net.minecraft.world.item.Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay displayComponent, java.util.function.Consumer<net.minecraft.network.chat.Component> textConsumer, net.minecraft.world.item.TooltipFlag type, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (at.hannibal2.skyhanni.utils.SkyBlockUtils.INSTANCE.getInSkyBlock() && at.hannibal2.skyhanni.SkyHanniMod.feature.getInventory().getEnchantParsing().getHideVanillaEnchants().get() && componentType == net.minecraft.core.component.DataComponents.ENCHANTMENTS) {
            ci.cancel();
        }
    }
    //#endif
}
