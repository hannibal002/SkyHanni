package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.minecraft.TexturesReloadEvent;
import at.hannibal2.skyhanni.features.misc.EmojiReplacer;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SimpleReloadableResourceManager.class)
public class MixinSimpleReloadableResourceManager {

    @Inject(
        at = @At("TAIL"),
        method = "reloadResources"
    )
    public void reloadResources(CallbackInfo ci) {
        //new TexturesReloadEvent().post();
        EmojiReplacer.INSTANCE.onTexturesLoad();
    }
}
