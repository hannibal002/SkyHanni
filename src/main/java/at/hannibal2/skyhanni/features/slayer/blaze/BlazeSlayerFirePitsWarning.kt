package at.hannibal2.skyhanni.features.slayer.blaze

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.util.render.TextRenderUtils
import at.hannibal2.skyhanni.events.BossHealthChangeEvent
import at.hannibal2.skyhanni.features.damageindicator.BossType
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class BlazeSlayerFirePitsWarning {

    companion object {
        private var textToRender = ""
        private var lastFirePitsWarning = 0L
        private var nextTickIn = 0

        fun fireFirePits() {
            lastFirePitsWarning = System.currentTimeMillis()
            textToRender = "§cFire Pits!"
            nextTickIn = 0
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return

        val difference = System.currentTimeMillis() - lastFirePitsWarning

        if (difference > 0) {
            if (difference > 2_000) {
                textToRender = ""
            } else {
                if (nextTickIn++ % 10 == 0) {
                    if (SkyHanniMod.feature.slayer.firePitsWarning) {
                        SoundUtils.createSound("random.orb", 0.8f).playSound()
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onBossHealthChange(event: BossHealthChangeEvent) {
        if (!isEnabled()) return
        val entityData = event.entityData

        val health = event.health
        val maxHealth = event.maxHealth
        val lastHealth = event.lastHealth

        val percentHealth = maxHealth * 0.33
        if (health < percentHealth) {
            if (lastHealth > percentHealth) {
                when (entityData.bossType) {
                    BossType.SLAYER_BLAZE_3,
                        //TODO blaze slayer tier 4
                        //BossType.SLAYER_BLAZE_4,
                    -> {
                        fireFirePits()
                    }

                    else -> {}
                }
            }
        }
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyblock && DamageIndicatorManager.isBossSpawned(
            BossType.SLAYER_BLAZE_3,
            //TODO blaze slayer tier 4
//            BossType.SLAYER_BLAZE_4,
            BossType.SLAYER_BLAZE_QUAZII_34,
            BossType.SLAYER_BLAZE_TYPHOEUS_34,
        )
    }

    @SubscribeEvent
    fun renderOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return
        if (!isEnabled()) return
        if (!SkyHanniMod.feature.slayer.firePitsWarning) return

        val scaledResolution = ScaledResolution(Minecraft.getMinecraft())
        val width = scaledResolution.scaledWidth
        val height = scaledResolution.scaledHeight

        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        val renderer = Minecraft.getMinecraft().fontRendererObj

        GlStateManager.pushMatrix()
        GlStateManager.translate((width / 2).toFloat(), (height / 1.8).toFloat(), 0.0f)
        GlStateManager.scale(4.0f, 4.0f, 4.0f)
        TextRenderUtils.drawStringCenteredScaledMaxWidth(textToRender, renderer, 0f, 0f, false, 75, 0)
        GlStateManager.popMatrix()
    }
}