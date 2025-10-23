package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHookKt;
import at.hannibal2.skyhanni.mixins.hooks.EntityRenderStateStore;
import at.hannibal2.skyhanni.mixins.hooks.GlowingStateStore;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.command.BatchingRenderCommandQueue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(BatchingRenderCommandQueue.class)
public class MixinBatchingRenderCommandQueue<E> {

    @WrapOperation(method = "submitItem", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private  boolean onSubmitItem(List<E> list, E itemCommand, Operation<Boolean> original) {
        EntityRenderStateStore currentState = EntityRenderDispatcherHookKt.getEntityRenderState();
        if (itemCommand instanceof GlowingStateStore casted && currentState != null && currentState.skyhanni$isUsingCustomOutline()) {
            casted.skyhanni$setUsingCustomOutline();
        }
        return original.call(list, itemCommand);
    }
}
