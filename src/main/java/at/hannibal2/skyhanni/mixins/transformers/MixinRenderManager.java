package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.SkyHanniMod;
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent;
import at.hannibal2.skyhanni.features.rift.DanceRoomHelper;
import at.hannibal2.skyhanni.utils.LorenzVec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderManager.class)
public class MixinRenderManager {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void shouldRender(Entity entity, ICamera camera, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
        if (new CheckRenderEntityEvent<>(entity, camera, camX, camY, camZ).postAndCatch()) {
            cir.setReturnValue(false);
        }

        if (DanceRoomHelper.INSTANCE.isEnabled() && SkyHanniMod.Companion.getFeature().rift.danceRoomHelper.hidePlayers) {
            for (Entity e : Minecraft.getMinecraft().theWorld.loadedEntityList) {
                if (e instanceof EntityOtherPlayerMP) {
                    LorenzVec vec = new LorenzVec(e.posX, e.posY, e.posZ);
                    if (DanceRoomHelper.INSTANCE.getDanceRoom().isVecInside(vec.toVec3())) {
                        cir.setReturnValue(false);
                    }
                }
            }
        }
    }
}
