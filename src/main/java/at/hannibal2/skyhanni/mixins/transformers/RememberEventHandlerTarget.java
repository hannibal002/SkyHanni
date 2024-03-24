package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.ASMEventHandlerExt;
import jline.internal.Nullable;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.ASMEventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import java.lang.reflect.Method;


@Mixin(ASMEventHandler.class)
public class RememberEventHandlerTarget implements ASMEventHandlerExt {
    private Object skyhanni_target;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInject(Object target, Method method, ModContainer owner) {
        skyhanni_target = target;
    }

    @Override
    public @Nullable Object getTarget_skyhanni() {
        return skyhanni_target;
    }
}
