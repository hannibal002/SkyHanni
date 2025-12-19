package at.hannibal2.skyhanni.mixins.transformers;

import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderLivingBase.class)
public interface AccessorRendererLivingEntity<T extends LivingEntity>
    extends AccessorRender<T> {
    @Invoker("setBrightness")
    boolean setBrightness_skyhanni(T entityLivingBaseIn, float partialTicks, boolean combineTextures);

    @Invoker("unsetBrightness")
    void unsetBrightness_skyhanni();
}
