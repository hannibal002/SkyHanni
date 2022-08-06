package at.hannibal2.skyhanni.dungeon.damageindicator

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.dungeon.DungeonData
import at.hannibal2.skyhanni.events.DamageIndicatorFinalBossEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.RenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.text.DecimalFormat
import java.util.*
import kotlin.math.max

class BossDamageIndicator {

    var data = mutableMapOf<EntityLivingBase, EntityData>()
    private var bossFinder: BossFinder? = null
    private val decimalFormat = DecimalFormat("0.0")
    private val maxHealth = mutableMapOf<UUID, Int>()

    @SubscribeEvent
    fun onDungeonStart(event: WorldEvent.Load) {
        bossFinder = BossFinder()
    }

    @SubscribeEvent(receiveCanceled = true)
    fun onChatMessage(event: LorenzChatEvent) {
        bossFinder?.handleChat(event.message)
    }

    @SubscribeEvent
    fun onWorldRender(event: RenderWorldLastEvent) {
        if (!SkyHanniMod.feature.dungeon.bossDamageIndicator) return

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
        try {
            val entity = event.entity
            val result = bossFinder?.shouldShow(entity) ?: return
            if (LorenzUtils.inDungeons) {
                checkLastBossDead(result.finalBoss, entity.entityId)
            }
            val ignoreBlocks = result.ignoreBlocks
            val delayedStart = result.delayedStart


            var health = event.entity.health.toInt()
            val maxHealth: Int
            if (DungeonData.isOneOf("F4")) {
                val hitPoints = when (health) {
                    300_000 -> 4
                    222_000 -> 3
                    144_000 -> 2
                    66_000 -> 1
                    else -> {
                        LorenzUtils.error("Unexpected health of thorn in F4! ($health)")
                        return
                    }
                }

                health = hitPoints
                maxHealth = 4
//            } else if (DungeonData.isOneOf("M4")) {
//                val hitPoints = when (health) {
//                    300_000 -> 4
//                    222_000 -> 3
//                    144_000 -> 2
//                    66_000 -> 1
//                    else -> {
//                        LorenzUtils.error("Unexpected health of thorn in F4! ($health)")
//                        return
//                    }
//                }

//                health = hitPoints
//                maxHealth = 4
            } else {
                val biggestHealth = getMaxHealthFor(event.entity)

                if (biggestHealth == 0) {
                    val currentMaxHealth = event.entity.baseMaxHealth.toInt()
                    maxHealth = max(currentMaxHealth, health)
                    setMaxHealth(event.entity, maxHealth)
                } else {
                    maxHealth = biggestHealth
                }
            }

            val percentage = health.toDouble() / maxHealth.toDouble()
            val color = when {
                percentage > 0.9 -> LorenzColor.DARK_GREEN
                percentage > 0.75 -> LorenzColor.GREEN
                percentage > 0.5 -> LorenzColor.YELLOW
                percentage > 0.25 -> LorenzColor.GOLD
                else -> LorenzColor.RED
            }

            if (data.containsKey(entity)){
                val lastHealth = data[entity]!!.lastHealth
                val diff = lastHealth -  health
                if (diff != 0) {
                    LorenzUtils.chat("diff: $diff")
                }

            }

            data[entity] = EntityData(
                entity,
                health,
                NumberUtil.format(health),
                color,
                System.currentTimeMillis(),
                ignoreBlocks,
                delayedStart
            )

        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun checkLastBossDead(finalBoss: Boolean, id: Int) {
        if (finalBoss) {
            DamageIndicatorFinalBossEvent(id).postAndCatch()
        }
    }

    private fun setMaxHealth(entity: EntityLivingBase, currentMaxHealth: Int) {
        maxHealth[entity.uniqueID!!] = currentMaxHealth
    }

    private fun getMaxHealthFor(entity: EntityLivingBase): Int {
        return maxHealth.getOrDefault(entity.uniqueID!!, 0)
    }

    @SubscribeEvent
    fun onWorldRender(event: EntityJoinWorldEvent) {
        bossFinder?.handleNewEntity(event.entity)
    }
}