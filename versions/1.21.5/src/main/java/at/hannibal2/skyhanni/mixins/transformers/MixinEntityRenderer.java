package at.hannibal2.skyhanni.mixins.transformers;

import at.hannibal2.skyhanni.events.SkyHanniRenderEntityEvent;
import at.hannibal2.skyhanni.mixins.hooks.EntityRenderDispatcherHookKt;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#if MC > 1.21.7
//$$ import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper;
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//$$ import net.minecraft.client.MinecraftClient;
//$$ import net.minecraft.client.render.command.OrderedRenderCommandQueue;
//$$ import net.minecraft.client.render.state.CameraRenderState;
//$$ import net.minecraft.entity.Entity;
//#endif

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    //#if MC < 1.21.9
    public void onRenderLabelHead(EntityRenderState state, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        //#else
        //$$ public void onRenderLabelHead(EntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraRenderState, CallbackInfo ci) {
        //#endif
        if (EntityRenderDispatcherHookKt.getEntity() instanceof LivingEntity livingEntity) {
            if (new SkyHanniRenderEntityEvent.Specials.Pre<>(livingEntity, state.x, state.y, state.z).post()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "renderLabelIfPresent", at = @At("TAIL"))
    //#if MC < 1.21.9
    public void onRenderLabelTail(EntityRenderState state, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        //#else
        //$$ public void onRenderLabelTail(EntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraRenderState, CallbackInfo ci) {
        //#endif
        if (EntityRenderDispatcherHookKt.getEntity() instanceof LivingEntity livingEntity) {
            new SkyHanniRenderEntityEvent.Specials.Post<>(livingEntity, state.x, state.y, state.z).post();
        }
    }

    //#if MC > 1.21.7
    //$$ @WrapOperation(method = "updateRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;hasOutline(Lnet/minecraft/entity/Entity;)Z"))
    //$$ public boolean shouldAlsoGlow(MinecraftClient instance, Entity entity, Operation<Boolean> original) {
    //$$     Integer glowColor = RenderLivingEntityHelper.getEntityGlowColor(entity);
    //$$     if (glowColor == null) {
    //$$         return original.call(instance, entity);
    //$$     }
    //$$     return true;
    //$$ }
    //$$
    //$$ @WrapOperation(method = "updateRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getTeamColorValue()I"))
    //$$ public int getCustomGlowColor(Entity instance, Operation<Integer> original) {
    //$$     Integer glowColor = RenderLivingEntityHelper.getEntityGlowColor(instance);
    //$$     if (glowColor == null) {
    //$$         return original.call(instance);
    //$$     }
    //$$     return glowColor;
    //$$ }
    //#endif



}
