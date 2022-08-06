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

    var data = mutableMapOf<UUID, EntityData>()
    private var bossFinder: BossFinder? = null
    private val decimalFormat = DecimalFormat("0.0")
    private val maxHealth = mutableMapOf<UUID, Int>()

    @SubscribeEvent
    fun onDungeonStart(event: WorldEvent.Load) {
        bossFinder = BossFinder()
        data.clear()
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
            if (System.currentTimeMillis() > data.timeLastTick + 100) continue//TODO use removeIf
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
            val entityData = grabData(entity) ?: return
            if (LorenzUtils.inDungeons) {
                checkFinalBoss(entityData.finalDungeonBoss, entity.entityId)
            }

            var health = entity.health.toInt()
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
                //TODO add m4 support
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

            if (data.containsKey(entity.uniqueID)) {
                val lastHealth = data[entity.uniqueID]!!.lastHealth
                val diff = lastHealth - health
                if (diff != 0) {
                    val bossType = entityData.bossType
                    if (bossType == BossType.NETHER_ASHFANG) {
                        if (diff < 0) {
                            val oldHealth = lastHealth / 1_000_000
                            val newHealth = health / 1_000_000
                            LorenzUtils.chat("§e§lAshfang healing: ${oldHealth}M -> ${newHealth}M")
                        }
                    } else if (bossType == BossType.NETHER_BLADESOUL) {
                        if (diff < 0) {
                            val oldHealth = lastHealth / 1_000_000
                            val newHealth = health / 1_000_000
                            val oldFormat = NumberUtil.format(oldHealth)
                            val newFormat = NumberUtil.format(newHealth)
                            LorenzUtils.chat("§e§lBladesoul healing: §a+12.5m §e(${oldFormat}M -> ${newFormat}M)")
                        }
                    } else if (bossType == BossType.NETHER_BARBARIAN_DUKE) {
                        if (diff < 0) {
                            val oldHealth = lastHealth / 1_000_000
                            val newHealth = health / 1_000_000
                            val oldFormat = NumberUtil.format(oldHealth)
                            val newFormat = NumberUtil.format(newHealth)
                            LorenzUtils.chat("§e§lBarbarian Duke healing: §a+12.5m §e(${oldFormat}M -> ${newFormat}M)")
                        }
                    } else {
                        LorenzUtils.chat("$bossType diff: $diff")
                    }
                }
            }

            entityData.lastHealth = health
            entityData.text = NumberUtil.format(health)
            entityData.color = color
            entityData.timeLastTick = System.currentTimeMillis()
            data[entity.uniqueID] = entityData

        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun grabData(entity: EntityLivingBase): EntityData? {
        if (data.contains(entity.uniqueID)) return data[entity.uniqueID]

        val entityResult = bossFinder?.tryAdd(entity) ?: return null
        return EntityData(
            entity,
            entityResult.ignoreBlocks,
            entityResult.delayedStart,
            entityResult.finalDungeonBoss,
            entityResult.bossType
        )
    }

    private fun checkFinalBoss(finalBoss: Boolean, id: Int) {
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