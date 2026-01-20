package at.hannibal2.skyhanni.mixins.transformers;
//? > 1.21.9 < 26.1 {
/*import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugEntryLocalDifficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugEntryLocalDifficulty.class)
public class MixinDebugEntryLocalDifficulty {

    @Inject(method = "display", at = @At(value = "HEAD"))
    public void render(DebugScreenDisplayer lines, Level world, LevelChunk clientChunk, LevelChunk chunk, CallbackInfo ci) {
        Minecraft minecraftClient = Minecraft.getInstance();
        Entity entity = minecraftClient.getCameraEntity();
        if (entity != null && minecraftClient.level != null && (chunk == null || world == null)) {
            long time = minecraftClient.level.getDayTime();
            lines.addLine("Local Difficulty: ?? (Day " + time / 24000L + ")");
        }
    }

}
*///?}
