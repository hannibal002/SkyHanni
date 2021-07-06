package com.thatgravyboat.skyblockhud.mixins;

import com.thatgravyboat.skyblockhud.SpecialColour;
import com.thatgravyboat.skyblockhud.handlers.sbentities.EntityTypeHelper;
import java.awt.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderEnderman;
import net.minecraft.entity.monster.EntityEnderman;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderEnderman.class)
public class MixinEndermanRenderer {

    @Inject(method = "doRender(Lnet/minecraft/entity/monster/EntityEnderman;DDDFF)V", at = @At("HEAD"))
    public void onRender(
        EntityEnderman entity,
        double x,
        double y,
        double z,
        float entityYaw,
        float partialTicks,
        CallbackInfo ci
    ) {
        if (EntityTypeHelper.isZealot(entity)) {
            Color color = new Color(SpecialColour.specialToChromaRGB("255:255:0:48:255"));
            GlStateManager.color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 255f);
        }
    }
}
