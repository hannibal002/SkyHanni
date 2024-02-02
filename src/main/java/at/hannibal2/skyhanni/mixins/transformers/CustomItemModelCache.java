// Taken with Permission from
// <https://git.nea.moe/nea/neuhax/src/branch/master/src/main/java/moe/nea/sky/mixin/patches/PatchCustomItemModelCache.java>
// under the LGPL 3.0 License

package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.features.misc.CacheResult;
import at.hannibal2.skyhanni.features.misc.OptifineCitCache;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.optifine.CustomItemProperties;
import net.optifine.CustomItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Pseudo
@Mixin(value = CustomItems.class, remap = false)
public class CustomItemModelCache {

    @Inject(method = "getCustomItemProperties", at = @At("HEAD"), cancellable = true)
    private static void overrideCustomItemProperties(
        ItemStack itemStack, int type,
        CallbackInfoReturnable<CustomItemProperties> cir) {
        CacheResult cacheHit = OptifineCitCache.retrieveCacheHit(itemStack, type);
        if (cacheHit != null) {
            cir.setReturnValue(cacheHit.getCustomItemProperties());
        }
    }

    @Inject(method = "getCustomItemProperties", at = @At(value = "RETURN", ordinal = 2),
        locals = LocalCapture.CAPTURE_FAILHARD)
    private static void storeCustomItemProperties(
        ItemStack itemStack, int type, CallbackInfoReturnable<CustomItemProperties> cir, Item item, int itemId, CustomItemProperties[] cips, int i, CustomItemProperties cip) {
        OptifineCitCache.storeCacheElement(itemStack, type, new CacheResult(cip));
    }

    @Inject(method = "getCustomItemProperties", at = @At(value = "RETURN", ordinal = 3))
    private static void storeCustomItemProperties(
        ItemStack itemStack, int type, CallbackInfoReturnable<CustomItemProperties> cir) {
        OptifineCitCache.storeCacheElement(itemStack, type, new CacheResult(null));
    }
}
