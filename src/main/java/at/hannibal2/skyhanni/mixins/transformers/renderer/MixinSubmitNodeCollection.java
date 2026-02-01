package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHookKt;
import at.hannibal2.skyhanni.mixins.hooks.EntityRenderStateStore;
import at.hannibal2.skyhanni.mixins.hooks.GlowingStateStore;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.SubmitNodeCollection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(SubmitNodeCollection.class)
public class MixinSubmitNodeCollection<E> {

    @WrapOperation(method = "submitItem", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private  boolean onSubmitItem(List<E> list, E itemCommand, Operation<Boolean> original) {
        EntityRenderStateStore currentState = EntityRenderDispatcherHookKt.getEntityRenderState();
        if (itemCommand instanceof GlowingStateStore casted && currentState != null && currentState.skyhanni$isUsingCustomOutline()) {
            casted.skyhanni$setUsingCustomOutline();
        }
        return original.call(list, itemCommand);
    }
}
