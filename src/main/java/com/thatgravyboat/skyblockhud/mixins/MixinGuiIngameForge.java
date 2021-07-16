package com.thatgravyboat.skyblockhud.mixins;

import static net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.*;

import com.thatgravyboat.skyblockhud.SkyblockHud;
import com.thatgravyboat.skyblockhud.Utils;
import com.thatgravyboat.skyblockhud.overlay.MiningHud;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngameForge.class)
public class MixinGuiIngameForge {

    @Shadow(remap = false)
    private RenderGameOverlayEvent eventParent;

    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true, remap = false)
    public void onRenderArmor(int width, int height, CallbackInfo ci) {
        if (SkyblockHud.config.renderer.hideArmor && SkyblockHud.hasSkyblockScoreboard()) {
            ci.cancel();
            if (prePost(ARMOR, eventParent)) return;
            postPost(ARMOR, eventParent);
        }
    }

    @Inject(method = "renderHealth", at = @At("HEAD"), cancellable = true, remap = false)
    public void onRenderHealth(int width, int height, CallbackInfo ci) {
        if (SkyblockHud.config.renderer.hideHearts && SkyblockHud.hasSkyblockScoreboard()) {
            ci.cancel();
            if (prePost(HEALTH, eventParent)) return;
            postPost(HEALTH, eventParent);
        }
    }

    @Inject(method = "renderAir", at = @At("HEAD"), cancellable = true, remap = false)
    public void onRenderAir(int width, int height, CallbackInfo ci) {
        if (SkyblockHud.config.renderer.hideAir && SkyblockHud.hasSkyblockScoreboard()) {
            ci.cancel();
            if (prePost(AIR, eventParent)) return;
            postPost(AIR, eventParent);
        }
    }

    @Inject(method = "renderHealthMount", at = @At("HEAD"), cancellable = true, remap = false)
    public void onRenderHealthMount(int width, int height, CallbackInfo ci) {
        if (SkyblockHud.config.renderer.hideAnimalHearts && SkyblockHud.hasSkyblockScoreboard()) {
            ci.cancel();
            if (prePost(HEALTHMOUNT, eventParent)) return;
            postPost(HEALTHMOUNT, eventParent);
        }
    }

    @Inject(method = "renderExperience", at = @At("HEAD"), cancellable = true, remap = false)
    public void onRenderExperience(int width, int height, CallbackInfo ci) {
        if (SkyblockHud.config.renderer.hideXpBar && SkyblockHud.hasSkyblockScoreboard()) {
            ci.cancel();
            if (prePost(EXPERIENCE, eventParent)) return;
            postPost(EXPERIENCE, eventParent);
        } else if (SkyblockHud.config.mining.barMode == 1) {
            if (!SkyblockHud.config.renderer.hideXpBar && (SkyblockHud.config.mining.showDrillBar || SkyblockHud.config.mining.showHeatBar) && SkyblockHud.hasSkyblockScoreboard()) {
                if (MiningHud.getHeat() > 0 || Utils.isDrill(Minecraft.getMinecraft().thePlayer.getHeldItem())) {
                    ci.cancel();
                    if (prePost(EXPERIENCE, eventParent)) return;
                    postPost(EXPERIENCE, eventParent);
                }
            }
        }
    }

    @Inject(method = "renderJumpBar", at = @At("HEAD"), cancellable = true, remap = false)
    public void onRenderJumpBar(int width, int height, CallbackInfo ci) {
        if (SkyblockHud.config.renderer.hideXpBar && SkyblockHud.hasSkyblockScoreboard()) {
            ci.cancel();
            if (prePost(JUMPBAR, eventParent)) return;
            postPost(JUMPBAR, eventParent);
        } else if (SkyblockHud.config.mining.barMode == 1) {
            if (!SkyblockHud.config.renderer.hideXpBar && (SkyblockHud.config.mining.showDrillBar || SkyblockHud.config.mining.showHeatBar) && SkyblockHud.hasSkyblockScoreboard()) {
                if (MiningHud.getHeat() > 0 || Utils.isDrill(Minecraft.getMinecraft().thePlayer.getHeldItem())) {
                    ci.cancel();
                    if (prePost(JUMPBAR, eventParent)) return;
                    postPost(JUMPBAR, eventParent);
                }
            }
        }
    }

    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true, remap = false)
    public void onRenderFood(int width, int height, CallbackInfo ci) {
        if (SkyblockHud.config.renderer.hideFood && SkyblockHud.hasSkyblockScoreboard()) {
            ci.cancel();
            if (prePost(FOOD, eventParent)) return;
            postPost(FOOD, eventParent);
        }
    }

    @Unique
    private static boolean prePost(RenderGameOverlayEvent.ElementType type, RenderGameOverlayEvent eventParent) {
        return MinecraftForge.EVENT_BUS.post(new RenderGameOverlayEvent.Pre(eventParent, type));
    }

    @Unique
    private static void postPost(RenderGameOverlayEvent.ElementType type, RenderGameOverlayEvent eventParent) {
        MinecraftForge.EVENT_BUS.post(new RenderGameOverlayEvent.Post(eventParent, type));
    }
}
