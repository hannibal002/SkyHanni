package at.hannibal2.skyhanni.damageindicator

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.dungeon.DungeonData
import at.hannibal2.skyhanni.events.DamageIndicatorFinalBossEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.misc.ScoreboardData
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.monster.EntityMagmaCube
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.text.DecimalFormat
import java.util.*
import kotlin.math.max

class BossDamageIndicator {

    var data = mutableMapOf<UUID, EntityData>()
    private var bossFinder: BossFinder? = null
    private val decimalFormat = DecimalFormat("0.0")
    private val maxHealth = mutableMapOf<UUID, Int>()

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        bossFinder = BossFinder()
        data.clear()
    }

    @SubscribeEvent(receiveCanceled = true)
    fun onChatMessage(event: LorenzChatEvent) {
        bossFinder?.handleChat(event.message)
    }

    @SubscribeEvent
    fun onWorldRender(event: RenderWorldLastEvent) {
        if (!SkyHanniMod.feature.misc.damageIndicator) return

        GlStateManager.disableDepth()
        GlStateManager.disableCull()

        val player = Minecraft.getMinecraft().thePlayer

        for (uuid in data.filter { System.currentTimeMillis() > it.value.timeLastTick + 100 }.map { it.key }) {
            data.remove(uuid)
        }

        for (data in data.values) {
            if (!data.ignoreBlocks) {
                if (!player.canEntityBeSeen(data.entity)) continue
            }
            if (data.hidden) continue

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
            val pos = Vec3(
                RenderUtils.interpolate(entity.posX, entity.lastTickPosX, partialTicks),
                RenderUtils.interpolate(entity.posY, entity.lastTickPosY, partialTicks) + 0.5f,
                RenderUtils.interpolate(entity.posZ, entity.lastTickPosZ, partialTicks)
            )
            RenderUtils.drawLabel(
                pos,
                color.getChatColor() + text,
                partialTicks,
                true,
                6f
            )

            var bossName = data.bossType.bossName
            if (data.namePrefix.isNotEmpty()) {
                bossName = data.namePrefix + bossName
            }
            if (data.nameSuffix.isNotEmpty()) {
                bossName += data.nameSuffix
            }

            RenderUtils.drawLabel(
                pos,
                bossName,
                partialTicks,
                true,
                3.9f,
                -9.0f
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
    fun onTickEvent(event: TickEvent.ClientTickEvent) {
        if (!LorenzUtils.inSkyblock) return
        for (entity in Minecraft.getMinecraft().theWorld.loadedEntityList) {
            if (entity is EntityLivingBase) {
                checkEntity(entity)
            }
        }
    }

    private fun checkEntity(entity: EntityLivingBase) {
        try {
            val entityData = grabData(entity) ?: return
            if (LorenzUtils.inDungeons) {
                checkFinalBoss(entityData.finalDungeonBoss, entity.entityId)
            }

            val biggestHealth = getMaxHealthFor(entity)

            val health = entity.health.toInt()
            val maxHealth: Int

            if (biggestHealth == 0) {
                val currentMaxHealth = entity.baseMaxHealth.toInt()
                maxHealth = max(currentMaxHealth, health)
                setMaxHealth(entity, maxHealth)
            } else {
                maxHealth = biggestHealth
            }

            var calcHealth = health
            var calcMaxHealth = maxHealth

            if (DungeonData.isOneOf("F4")) {
                calcHealth = when (health) {
                    300_000 -> 4
                    222_000 -> 3
                    144_000 -> 2
                    66_000 -> 1
                    else -> {
                        LorenzUtils.error("Unexpected health of thorn in F4! ($health)")
                        return
                    }
                }
                calcMaxHealth = 4
                //TODO add m4 support
//            } else if (DungeonData.isOneOf("M4")) {
//                calcHealth = when (health) {
//                    300_000 -> 4
//                    222_000 -> 3
//                    144_000 -> 2
//                    66_000 -> 1
//                    else -> {
//                        LorenzUtils.error("Unexpected health of thorn in F4! ($health)")
//                        return
//                    }
//                }
//                calcMaxHealth = 4
            }


            if (entityData.bossType == BossType.END_ENDERMAN_SLAYER) {
                var statePrefix = ""
                //Hides the damage indicator when in hit phase or in laser phase
                if (entity is EntityEnderman) {
                    var hidden = false
                    val hasNameTagWith = entity.hasNameTagWith(3, " Hit")
//                    println("is in hit phase: $hasNameTagWith")
                    if (hasNameTagWith) hidden = true
                    if (entity.ridingEntity != null) hidden = true
                    entityData.hidden = hidden
                }

                if (!entityData.hidden) {
                    //custom prefix and health for the four different ender slayers
                    when (maxHealth) {
                        300_000_000, 600_000_000 -> {
                            entityData.namePrefix = "§4"
                            entityData.nameSuffix = " 4"
                            val step = maxHealth / 6
                            calcMaxHealth = step
                            if (health > step * 5) {
                                calcHealth -= step * 5
                                statePrefix = "§c1/6 "
                            } else if (health > step * 4) {
                                calcHealth -= step * 4
                                statePrefix = "§e2/6 "
                            } else if (health > step * 3) {
                                calcHealth -= step * 3
                                statePrefix = "§e3/6 "
                            } else if (health > step * 2) {
                                calcHealth -= step * 2
                                statePrefix = "§e4/6 "
                            } else if (health > step) {
                                calcHealth -= step
                                statePrefix = "§e5/6 "
                            } else {
                                calcHealth = health
                                statePrefix = "§a6/6 "
                            }
                        }
                        else -> {
                            when (maxHealth) {
                                300_000, 600_000 -> {
                                    entityData.namePrefix = "§a"
                                    entityData.nameSuffix = " 1"
                                }
                                15_000_000, 30_000_000 -> {
                                    entityData.namePrefix = "§e"
                                    entityData.nameSuffix = " 2"
                                }
                                66_666_666, 66_666_666 * 2 -> {
                                    entityData.namePrefix = "§c"
                                    entityData.nameSuffix = " 3"
                                }
                            }

                            val step = maxHealth / 3

                            calcMaxHealth = step
                            if (health > step * 2) {
                                calcHealth -= step * 2
                                statePrefix = "§c1/3 "
                            } else if (health > step) {
                                calcHealth -= step
                                statePrefix = "§e2/3 "
                            } else {
                                calcHealth = health
                                statePrefix = "§a3/3 "
                            }
                        }

                    }
                }
                entityData.namePrefix = statePrefix + entityData.namePrefix
            }
            if (entityData.bossType == BossType.NETHER_MAGMA_BOSS) {
                if (entity is EntityMagmaCube) {
                    val slimeSize = entity.slimeSize
                    entityData.namePrefix = when (slimeSize) {
                        24 -> "§c1/6"
                        22 -> "§e2/6"
                        20 -> "§e3/6"
                        18 -> "§e4/6"
                        16 -> "§e5/6"
                        else -> {
                            calcMaxHealth = 10_000_000
                            "§a6/6"
                        }
                    } + " §f"

                    //hide while in the middle
                    val position = entity.getLorenzVec()
                    entityData.hidden = position.x == -368.0 && position.z == -804.0

                    for (line in ScoreboardData.sidebarLinesRaw) {
                        if (line.contains("▎")) {
                            val color: String
                            if (line.startsWith("§7")) {
                                color = "§7"
                            } else if (line.startsWith("§e")) {
                                color = "§e"
                            } else if (line.startsWith("§6") || line.startsWith("§a") || line.startsWith("§c")) {
                                calcHealth = 0
                                break
                            } else {
                                LorenzUtils.error("unknown magma boss health sidebar format!")
                                break
                            }

                            val text = line.replace("\uD83C\uDF81" + color, "")
                            val max = 25.0
                            val length = text.split("§e", "§7")[1].length
                            val missing = (health.toDouble() / max) * length
                            calcHealth = (health - missing).toInt()
                        }
                    }
                }
            }

            val percentage = calcHealth.toDouble() / calcMaxHealth.toDouble()
            val color = when {
                percentage > 0.9 -> LorenzColor.DARK_GREEN
                percentage > 0.75 -> LorenzColor.GREEN
                percentage > 0.5 -> LorenzColor.YELLOW
                percentage > 0.25 -> LorenzColor.GOLD
                else -> LorenzColor.RED
            }

            if (SkyHanniMod.feature.misc.damageIndicatorHealingMessage) {
                if (data.containsKey(entity.uniqueID)) {
                    val lastHealth = data[entity.uniqueID]!!.lastHealth
                    val bossType = entityData.bossType
                    checkHealed(health, lastHealth, bossType)
                }
            }

            entityData.lastHealth = health
            entityData.text = NumberUtil.format(calcHealth)
            entityData.color = color
            entityData.timeLastTick = System.currentTimeMillis()
            data[entity.uniqueID] = entityData

        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun checkHealed(health: Int, lastHealth: Int, bossType: BossType) {
        val healed = health - lastHealth
        if (healed <= 0) return

        //Hide auto heal every 10 ticks
        if (healed == 15_000 && bossType == BossType.HUB_REVENANT_HORROR) return

        val formatLastHealth = NumberUtil.format(lastHealth)
        val formatHealth = NumberUtil.format(health)
        val healedFormat = NumberUtil.format(healed)
        println(bossType.bossName + " §fhealed for $healed❤ ($lastHealth -> $health)")
        LorenzUtils.chat(bossType.bossName + " §ehealed for §a$healedFormat❤ §8(§e$formatLastHealth -> $formatHealth§8)")
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