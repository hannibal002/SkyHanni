package at.hannibal2.skyhanni.mixins.transformers;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import java.util.List;

@Mixin(DebugHud.class)
public class MixinDebugHud {

    @Shadow
    @Final
    private MinecraftClient client;

    @WrapOperation(method = "getLeftText", slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;getBiome(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/registry/entry/RegistryEntry;")), at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 2))
    public <E> boolean addDay(List instance, E e, Operation<Boolean> original) {
        long time = client.world.getTimeOfDay();
        if (time == 0) return original.call(instance, e);
        instance.add("Local Difficulty: ?? (Day " + time / 24000L + ")");
        return false;
    }
}
