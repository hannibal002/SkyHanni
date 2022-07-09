package at.lorenz.mod.dungeon.damageindicator

import at.lorenz.mod.LorenzMod
import at.lorenz.mod.events.DungeonEnterEvent
import at.lorenz.mod.events.LorenzChatEvent
import at.lorenz.mod.utils.LorenzColor
import at.lorenz.mod.utils.LorenzUtils
import at.lorenz.mod.utils.LorenzUtils.baseMaxHealth
import at.lorenz.mod.utils.NumberUtil
import at.lorenz.mod.utils.RenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.text.DecimalFormat
import java.util.*
import kotlin.math.max

class DungeonBossDamageIndicator {

    var data = mutableMapOf<EntityLivingBase, EntityData>()
    private var bossFinder: DungeonBossFinder? = null
    private val decimalFormat = DecimalFormat("0.0")

    @SubscribeEvent
    fun onDungeonStart(event: DungeonEnterEvent) {
        bossFinder = DungeonBossFinder()
    }

    @SubscribeEvent(receiveCanceled = true)
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.inDungeons) return
        if (!LorenzMod.feature.dungeon.bossDamageIndicator) return

        bossFinder?.handleChat(event.message)
    }

    @SubscribeEvent
    fun onWorldRender(event: RenderWorldLastEvent) {
        GlStateManager.disableDepth()
        GlStateManager.disableCull()

        val player = Minecraft.getMinecraft().thePlayer

        for (data in data.values) {
            if (System.currentTimeMillis() > data.time + 100) continue//TODO use removeIf
            if (!data.ignoreBlocks) {
                if (!player.canEntityBeSeen(data.entity)) continue
            }

            val entity = data.entity

            var color = data.color
            var text = data.text
            val delayedStart = data.delayedStart
            if (delayedStart != -1L) {
                if (delayedStart > System.currentTimeMillis()) {
                    val delay = delayedStart - System.currentTimeMillis()
                    color = colorForTime(delay)
                    var d = delay * 1.0
                    d /= 1000
                    text = decimalFormat.format(d)
                }
            }

            val partialTicks = event.partialTicks
            RenderUtils.drawLabel(
                Vec3(
                    RenderUtils.interpolate(entity.posX, entity.lastTickPosX, partialTicks),
                    RenderUtils.interpolate(entity.posY, entity.lastTickPosY, partialTicks) + 0.5f,
                    RenderUtils.interpolate(entity.posZ, entity.lastTickPosZ, partialTicks)
                ),
                text,
                color.toColor(),
                partialTicks,
                true,
                6f
            )
        }
        GlStateManager.enableDepth()
        GlStateManager.enableCull()
    }

    private fun colorForTime(delayedStart: Long): LorenzColor = when {
        delayedStart < 1_000 -> LorenzColor.DARK_PURPLE
        delayedStart < 3_000 -> LorenzColor.LIGHT_PURPLE

        else -> LorenzColor.WHITE
    }

    @SubscribeEvent
    fun onRenderLivingPost(event: RenderLivingEvent.Post<*>) {
        if (!LorenzUtils.inDungeons) return
        if (!LorenzMod.feature.dungeon.bossDamageIndicator) return

        try {
            val entity = event.entity

            var ignoreBlocks = false
            var delayedStart = -1L
            val show = bossFinder?.shouldShow(entity, { ignoreBlocks = it }, { delayedStart = it }) ?: false
            if (!show) return

            val currentMaxHealth = event.entity.baseMaxHealth

            val debugMaxHealth = getMaxHealthFor(event.entity)
            val biggestHealth = max(currentMaxHealth, debugMaxHealth)
            if (biggestHealth > debugMaxHealth) {
                setMaxHealth(event.entity, biggestHealth)
            }

            val percentage = event.entity.health / max(debugMaxHealth, currentMaxHealth)
            val color = when {
                percentage > 0.9 -> LorenzColor.DARK_GREEN
                percentage > 0.75 -> LorenzColor.GREEN
                percentage > 0.5 -> LorenzColor.YELLOW
                percentage > 0.25 -> LorenzColor.GOLD
                else -> LorenzColor.RED
            }

            data[entity] = EntityData(
                entity,
                NumberUtil.format(event.entity.health),
                color,
                System.currentTimeMillis(),
                ignoreBlocks,
                delayedStart
            )

        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    //TODO test why this does not work
    val maxHealth = mutableMapOf<UUID, Double>()

    private fun setMaxHealth(entity: EntityLivingBase, currentMaxHealth: Double) {
        maxHealth[entity.uniqueID!!] = currentMaxHealth
    }

    private fun getMaxHealthFor(entity: EntityLivingBase): Double {
        return maxHealth.getOrDefault(entity.uniqueID!!, 0.0)
    }

    @SubscribeEvent
    fun onWorldRender(event: EntityJoinWorldEvent) {
        bossFinder?.handleNewEntity(event.entity)
    }
}