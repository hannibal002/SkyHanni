package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.minecraft.ComponentsLoadedEvent;
import net.minecraft.server.ReloadableServerResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ReloadableServerResources.class)
public class MixinReloadableServerResources {

    @Inject(method = "updateComponentsAndStaticRegistryTags", at = @At("TAIL"))
    private void skyhanni$onComponentsBound(CallbackInfo ci) {
        ComponentsLoadedEvent.INSTANCE.post();
    }
}
