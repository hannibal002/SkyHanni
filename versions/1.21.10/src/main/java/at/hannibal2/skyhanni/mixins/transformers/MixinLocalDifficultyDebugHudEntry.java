package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.debug.DebugHudLines;
import net.minecraft.client.gui.hud.debug.LocalDifficultyDebugHudEntry;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalDifficultyDebugHudEntry.class)
public class MixinLocalDifficultyDebugHudEntry {

    @Inject(method = "render", at = @At(value = "HEAD"))
    public void render(DebugHudLines lines, World world, WorldChunk clientChunk, WorldChunk chunk, CallbackInfo ci) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Entity entity = minecraftClient.getCameraEntity();
        if (entity != null && minecraftClient.world != null && (chunk == null || world == null)) {
            long time = minecraftClient.world.getTimeOfDay();
            lines.addLine("Local Difficulty: ?? (Day " + time / 24000L + ")");
        }
    }

}