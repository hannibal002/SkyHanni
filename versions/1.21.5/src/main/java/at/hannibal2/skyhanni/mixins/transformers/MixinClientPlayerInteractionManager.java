package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.data.ClickType;
import at.hannibal2.skyhanni.events.BlockClickEvent;
import at.hannibal2.skyhanni.events.ItemClickEvent;
import at.hannibal2.skyhanni.utils.InventoryUtils;
import at.hannibal2.skyhanni.utils.LorenzVec;
import at.hannibal2.skyhanni.utils.LorenzVecKt;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManager {


    @Inject(method = "attackBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getTutorialManager()Lnet/minecraft/client/tutorial/TutorialManager;", ordinal = 1), cancellable = true)
    public void attackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        LorenzVec position = LorenzVecKt.toLorenzVec(pos);
        boolean blockClickCancelled = new BlockClickEvent(ClickType.LEFT_CLICK, position, InventoryUtils.INSTANCE.getItemInHand()).post();
        ItemClickEvent itemClickEvent = new ItemClickEvent(InventoryUtils.INSTANCE.getItemInHand(), ClickType.LEFT_CLICK);
        if (blockClickCancelled) itemClickEvent.cancel();
        boolean itemClickCancelled = itemClickEvent.post();
        if (blockClickCancelled || itemClickCancelled) cir.setReturnValue(false);

    }

}
