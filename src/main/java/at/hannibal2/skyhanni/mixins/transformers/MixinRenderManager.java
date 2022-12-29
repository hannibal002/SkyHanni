package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.CheckRenderEntityEvent;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.UUID;

@Mixin(RenderManager.class)
public class MixinRenderManager {

    private final HashMap<UUID, Long> lastColorCacheTime = new HashMap<>();
    private final HashMap<UUID, Boolean> cache = new HashMap<>();

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void shouldRender(Entity entity, ICamera camera, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
        UUID uuid = entity.getUniqueID();
        boolean shouldRender;
        if (lastColorCacheTime.getOrDefault(uuid, 0L) + 1_000 > System.currentTimeMillis()) {
            shouldRender = cache.get(uuid);
        } else {
            shouldRender = !new CheckRenderEntityEvent<>(entity, camera, camX, camY, camZ).postAndCatch();

            cache.put(uuid, shouldRender);
            lastColorCacheTime.put(uuid, System.currentTimeMillis());
        }

        cir.setReturnValue(shouldRender);
    }
}
