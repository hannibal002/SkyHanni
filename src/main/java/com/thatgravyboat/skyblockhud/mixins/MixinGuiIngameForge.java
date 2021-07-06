package com.thatgravyboat.skyblockhud.mixins;

import static net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.*;

import com.thatgravyboat.skyblockhud.SkyblockHud;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngameForge.class)
public class MixinGuiIngameForge {

    @Shadow(remap = false)
    private RenderGameOverlayEvent eventParent;

    @Inject(
        method = "renderArmor",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    public void onRenderArmor(int width, int height, CallbackInfo ci) {
        if (
            SkyblockHud.config.renderer.hideArmor &&
            SkyblockHud.hasSkyblockScoreboard()
        ) {
            ci.cancel();
            if (pre(ARMOR)) return;
            post(ARMOR);
        }
    }

    @Inject(
        method = "renderHealth",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    public void onRenderHealth(int width, int height, CallbackInfo ci) {
        if (
            SkyblockHud.config.renderer.hideHearts &&
            SkyblockHud.hasSkyblockScoreboard()
        ) {
            ci.cancel();
            if (pre(HEALTH)) return;
            post(HEALTH);
        }
    }

    @Inject(
        method = "renderAir",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    public void onRenderAir(int width, int height, CallbackInfo ci) {
        if (
            SkyblockHud.config.renderer.hideAir &&
            SkyblockHud.hasSkyblockScoreboard()
        ) {
            ci.cancel();
            if (pre(AIR)) return;
            post(AIR);
        }
    }

    @Inject(
        method = "renderHealthMount",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    public void onRenderHealthMount(int width, int height, CallbackInfo ci) {
        if (
            SkyblockHud.config.renderer.hideAnimalHearts &&
            SkyblockHud.hasSkyblockScoreboard()
        ) {
            ci.cancel();
            if (pre(HEALTHMOUNT)) return;
            post(HEALTHMOUNT);
        }
    }

    @Inject(
        method = "renderExperience",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    public void onRenderExperience(int width, int height, CallbackInfo ci) {
        if (
            SkyblockHud.config.renderer.hideXpBar &&
            SkyblockHud.hasSkyblockScoreboard()
        ) {
            ci.cancel();
            if (pre(EXPERIENCE)) return;
            post(EXPERIENCE);
        }
    }

    @Inject(
        method = "renderJumpBar",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    public void onRenderJumpBar(int width, int height, CallbackInfo ci) {
        if (
            SkyblockHud.config.renderer.hideXpBar &&
            SkyblockHud.hasSkyblockScoreboard()
        ) {
            ci.cancel();
            if (pre(JUMPBAR)) return;
            post(JUMPBAR);
        }
    }

    @Inject(
        method = "renderFood",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    public void onRenderFood(int width, int height, CallbackInfo ci) {
        if (
            SkyblockHud.config.renderer.hideFood &&
            SkyblockHud.hasSkyblockScoreboard()
        ) {
            ci.cancel();
            if (pre(FOOD)) return;
            post(FOOD);
        }
    }

    private boolean pre(RenderGameOverlayEvent.ElementType type) {
        return MinecraftForge.EVENT_BUS.post(
            new RenderGameOverlayEvent.Pre(eventParent, type)
        );
    }

    private void post(RenderGameOverlayEvent.ElementType type) {
        MinecraftForge.EVENT_BUS.post(
            new RenderGameOverlayEvent.Post(eventParent, type)
        );
    }
}
