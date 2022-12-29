package at.hannibal2.skyhanni.mixins.transformers.renderer;

import at.hannibal2.skyhanni.events.RenderMobColoredEvent;
import at.hannibal2.skyhanni.events.ResetEntityHurtEvent;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.UUID;

//TODO make guava caches working
@Mixin(RendererLivingEntity.class)
public abstract class MixinRendererLivingEntity<T extends EntityLivingBase> extends Render<T> {

    private final HashMap<UUID, Long> lastColorCacheTime = new HashMap<>();
    private final HashMap<UUID, Integer> cachedColor = new HashMap<>();

    private final HashMap<UUID, Long> lastHurtCacheTime = new HashMap<>();
    private final HashMap<UUID, Boolean> cachedHurt = new HashMap<>();

    //    private final LoadingCache<EntityLivingBase, Integer> colorMultiplier;
    protected MixinRendererLivingEntity(RenderManager renderManager) {
        super(renderManager);

//        colorMultiplier = CacheBuilder.newBuilder()
////                .maximumSize(1000)
//                .expireAfterWrite(2, TimeUnit.SECONDS)
//                .build(
//                        new CacheLoader<EntityLivingBase, Integer>() {
//                            @Override
//                            public Integer load(@NotNull EntityLivingBase key) {
//                                RenderMobColoredEvent event = new RenderMobColoredEvent(key, 0);
//                                event.postAndCatch();
//                                return event.getColor();
//                            }
//                        }
//                );
    }

    @Inject(method = "getColorMultiplier", at = @At("HEAD"), cancellable = true)
    private void setColorMultiplier(T entity, float lightBrightness, float partialTickTime, CallbackInfoReturnable<Integer> cir) {
        UUID uuid = entity.getUniqueID();
        if (lastColorCacheTime.getOrDefault(uuid, 0L) + 1_000 > System.currentTimeMillis()) {
            cir.setReturnValue(cachedColor.get(uuid));
            return;
        }

        RenderMobColoredEvent event = new RenderMobColoredEvent(entity, 0);
        event.postAndCatch();
        int color = event.getColor();
        cachedColor.put(uuid, color);
        lastColorCacheTime.put(uuid, System.currentTimeMillis());
        cir.setReturnValue(color);

//        try {
//            cir.setReturnValue(colorMultiplier.get(entity));
//        } catch (ExecutionException e) {
//            throw new RuntimeException(e);
//        }
    }

    @Redirect(method = "setBrightness", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/EntityLivingBase;hurtTime:I", opcode = Opcodes.GETFIELD))
    private int changeHurtTime(EntityLivingBase entity) {
        UUID uuid = entity.getUniqueID();

        boolean shouldReset;
        if (lastHurtCacheTime.getOrDefault(uuid, 0L) + 1_000 > System.currentTimeMillis()) {
            shouldReset = cachedHurt.get(uuid);
        } else {
            ResetEntityHurtEvent event = new ResetEntityHurtEvent(entity, false);
            event.postAndCatch();
            shouldReset = event.getShouldReset();
            cachedHurt.put(uuid, shouldReset);
            lastHurtCacheTime.put(uuid, System.currentTimeMillis());
        }

        return shouldReset ? 0 : entity.hurtTime;
    }
}