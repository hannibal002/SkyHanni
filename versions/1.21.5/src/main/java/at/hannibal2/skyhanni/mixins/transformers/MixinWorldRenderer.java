package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper;
import at.hannibal2.skyhanni.utils.EntityUtils;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {

    @WrapOperation(method = {"renderEntities", "getEntitiesToRender"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;hasOutline(Lnet/minecraft/entity/Entity;)Z"))
    public boolean shouldAlsoGlow(MinecraftClient instance, Entity entity, Operation<Boolean> original) {
        if (entity instanceof LivingEntity livingEntity) {
            int i = RenderLivingEntityHelper.internalSetColorMultiplier(livingEntity,0);
            if (i == 0) return original.call(instance, entity);
            return EntityUtils.INSTANCE.canBeSeen(entity, 150, .5);
        }
        return original.call(instance, entity);
    }

    @WrapOperation(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getTeamColorValue()I"))
    public int changeGlowColour(Entity entity, Operation<Integer> original) {
        if (entity instanceof LivingEntity livingEntity) {
            int i = RenderLivingEntityHelper.internalSetColorMultiplier(livingEntity, 0);
            if (i == 0) return original.call(entity);
            return i;
        }
        return original.call(entity);
    }

}
