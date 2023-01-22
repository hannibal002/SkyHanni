package at.hannibal2.skyhanni.features.damageindicator

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.BossHealthChangeEvent
import at.hannibal2.skyhanni.events.DamageIndicatorFinalBossEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonData
import at.hannibal2.skyhanni.features.slayer.blaze.HellionShield
import at.hannibal2.skyhanni.features.slayer.blaze.setHellionShield
import at.hannibal2.skyhanni.test.LorenzTest
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.EntityUtils.getNameTagWith
import at.hannibal2.skyhanni.utils.EntityUtils.hasNameTagWith
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.LorenzUtils.between
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLiving
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.entity.monster.EntityMagmaCube
import net.minecraft.entity.monster.EntityZombie
import net.minecraft.entity.passive.EntityWolf
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.text.DecimalFormat
import java.util.*
import java.util.regex.Pattern
import kotlin.math.max

class DamageIndicatorManager {

    private var mobFinder: MobFinder? = null
    private val decimalFormat = DecimalFormat("0.0")
    private val maxHealth = mutableMapOf<UUID, Long>()

    companion object {
        private var data = mutableMapOf<UUID, EntityData>()
        private val damagePattern = Pattern.compile("[✧✯]?(\\d+[⚔+✧❤♞☄✷ﬗ✯]*)")

        fun isBoss(entity: EntityLivingBase): Boolean {
            return data.values.any { it.entity == entity }
        }

        fun isDamageSplash(entity: EntityLivingBase): Boolean {
            if (entity.ticksExisted > 300 || entity !is EntityArmorStand) return false
            if (!entity.hasCustomName()) return false
            if (entity.isDead) return false
            val name = entity.customNameTag.removeColor().replace(",", "")

            return damagePattern.matcher(name).matches()
        }

        fun isBossSpawned(type: BossType): Boolean {
            return data.entries.find { it.value.bossType == type } != null
        }

        fun isBossSpawned(vararg types: BossType): Boolean {
            return types.any { isBossSpawned(it) }
        }

        fun getDistanceTo(vararg types: BossType): Double {
            val playerLocation = LocationUtils.playerLocation()
            val list =
                data.values.filter { it.bossType in types }.map { it.entity.getLorenzVec().distance(playerLocation) }
            return if (list.isEmpty()) {
                Double.MAX_VALUE
            } else {
                list.minOf { it }
            }
        }

        fun getNearestDistanceTo(location: LorenzVec): Double {
            return data.values
                .map { it.entity.getLorenzVec() }
                .minOfOrNull { it.distance(location) } ?: Double.MAX_VALUE
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        mobFinder = MobFinder()
        data.clear()
    }

    @SubscribeEvent(receiveCanceled = true)
    fun onChatMessage(event: LorenzChatEvent) {
        mobFinder?.handleChat(event.message)
    }

    @SubscribeEvent
    fun onWorldRender(event: RenderWorldLastEvent) {
        if (!isEnabled()) return

        GlStateManager.disableDepth()
        GlStateManager.disableCull()

        val player = Minecraft.getMinecraft().thePlayer

        //TODO config to define between 100ms and 5 sec
        for (uuid in data.filter {
            val waitForRemoval = if (it.value.dead && !noDeathDisplay(it.value.bossType)) 4_000 else 100
            (System.currentTimeMillis() > it.value.timeLastTick + waitForRemoval) || (it.value.dead && noDeathDisplay(it.value.bossType))
        }.map { it.key }) {
            data.remove(uuid)
        }

        val sizeHealth: Double
        val sizeNameAbove: Double
        val sizeBossName: Double
        val sizeFinalResults: Double
        val smallestDistanceVew: Double
        val thirdPersonView = Minecraft.getMinecraft().gameSettings.thirdPersonView
        // 0 == normal
        // 1 == f3 behind
        // 2 == selfie
        if (thirdPersonView == 1) {
            sizeHealth = 2.8
            sizeNameAbove = 2.2
            sizeBossName = 2.4
            sizeFinalResults = 1.8

            smallestDistanceVew = 10.0
        } else {
            sizeHealth = 1.9
            sizeNameAbove = 1.8
            sizeBossName = 2.1
            sizeFinalResults = 1.4

            smallestDistanceVew = 6.0
        }

        for (data in data.values) {

            //TODO test end stone protector in hole? - maybe change eye pos
//            data.ignoreBlocks =
//                data.bossType == BossType.END_ENDSTONE_PROTECTOR && Minecraft.getMinecraft().thePlayer.isSneaking

            if (!data.ignoreBlocks) {
                if (!player.canEntityBeSeen(data.entity)) continue
            }
            if (data.bossType.bossTypeToggle !in SkyHanniMod.feature.damageIndicator.bossesToShow) continue

            val entity = data.entity

            var healthText = data.healthText
            val delayedStart = data.delayedStart
            if (delayedStart != -1L) {
                if (delayedStart > System.currentTimeMillis()) {
                    val delay = delayedStart - System.currentTimeMillis()
                    healthText = formatDelay(delay)
                }
            }

            val partialTicks = event.partialTicks
            val location = if (data.dead && data.deathLocation != null) {
                data.deathLocation!!
            } else {
                val loc = LorenzVec(
                    RenderUtils.interpolate(entity.posX, entity.lastTickPosX, partialTicks),
                    RenderUtils.interpolate(entity.posY, entity.lastTickPosY, partialTicks) + 0.5f,
                    RenderUtils.interpolate(entity.posZ, entity.lastTickPosZ, partialTicks)
                )
                if (data.dead) data.deathLocation = loc
                loc
            }.add(-0.5, 0.0, -0.5)


            event.drawDynamicText(location, healthText, sizeHealth, smallestDistanceVew = smallestDistanceVew)

            if (data.nameAbove.isNotEmpty()) {
                event.drawDynamicText(location, data.nameAbove, sizeNameAbove, -18f, smallestDistanceVew = smallestDistanceVew)
            }

            var bossName = when (SkyHanniMod.feature.damageIndicator.bossName) {
                0 -> ""
                1 -> data.bossType.fullName
                2 -> data.bossType.shortName
                else -> data.bossType.fullName
            }

            if (data.namePrefix.isNotEmpty()) {
                bossName = data.namePrefix + bossName
            }
            if (data.nameSuffix.isNotEmpty()) {
                bossName += data.nameSuffix
            }
            event.drawDynamicText(location, bossName, sizeBossName, -9f, smallestDistanceVew = smallestDistanceVew)

            if (SkyHanniMod.feature.damageIndicator.showDamageOverTime) {
                var diff = 13f
                val currentDamage = data.damageCounter.currentDamage
                val currentHealing = data.damageCounter.currentHealing
                if (currentDamage != 0L || currentHealing != 0L) {
                    val formatDamage = "§c" + NumberUtil.format(currentDamage)
                    val formatHealing = "§a+" + NumberUtil.format(currentHealing)
                    val finalResult = if (currentHealing == 0L) {
                        formatDamage
                    } else if (currentDamage == 0L) {
                        formatHealing
                    } else {
                        "$formatDamage §7/ $formatHealing"
                    }
                    event.drawDynamicText(location, finalResult, sizeFinalResults, diff, smallestDistanceVew = smallestDistanceVew)
                    diff += 9f
                }
                for (damage in data.damageCounter.oldDamages) {
                    val formatDamage = "§c" + NumberUtil.format(damage.damage) + "/s"
                    val formatHealing = "§a+" + NumberUtil.format(damage.healing) + "/s"
                    val finalResult = if (damage.healing == 0L) {
                        formatDamage
                    } else if (damage.damage == 0L) {
                        formatHealing
                    } else {
                        "$formatDamage §7/ $formatHealing"
                    }
                    event.drawDynamicText(location, finalResult, sizeFinalResults, diff, smallestDistanceVew = smallestDistanceVew)
                    diff += 9f
                }
            }

        }
        GlStateManager.enableDepth()
        GlStateManager.enableCull()
    }

    private fun noDeathDisplay(bossType: BossType): Boolean {
        return when (bossType) {
            BossType.SLAYER_BLAZE_TYPHOEUS_1,
            BossType.SLAYER_BLAZE_TYPHOEUS_2,
            BossType.SLAYER_BLAZE_TYPHOEUS_3,
            BossType.SLAYER_BLAZE_TYPHOEUS_4,
            BossType.SLAYER_BLAZE_QUAZII_1,
            BossType.SLAYER_BLAZE_QUAZII_2,
            BossType.SLAYER_BLAZE_QUAZII_3,
            BossType.SLAYER_BLAZE_QUAZII_4,

                //TODO f3/m3 4 guardians, f2/m2 4 boss room fighters
            -> true

            else -> false
        }
    }

    private fun tickDamage(damageCounter: DamageCounter) {
        val now = System.currentTimeMillis()
        if (damageCounter.currentDamage != 0L || damageCounter.currentHealing != 0L) {
            if (damageCounter.firstTick == 0L) {
                damageCounter.firstTick = now
            }

            if (now > damageCounter.firstTick + 1_000) {
                damageCounter.oldDamages.add(
                    0, OldDamage(now, damageCounter.currentDamage, damageCounter.currentHealing)
                )
                damageCounter.firstTick = 0L
                damageCounter.currentDamage = 0
                damageCounter.currentHealing = 0
            }
        }
        damageCounter.oldDamages.removeIf { now > it.time + 5_000 }
    }

    private fun formatDelay(delay: Long): String {
        val color = when {
            delay < 1_000 -> LorenzColor.DARK_PURPLE
            delay < 3_000 -> LorenzColor.LIGHT_PURPLE

            else -> LorenzColor.WHITE
        }
        val d = (delay * 1.0) / 1000
        return color.getChatColor() + decimalFormat.format(d)
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!LorenzUtils.inSkyBlock) return
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

            val health = entity.health.toLong()
            val maxHealth: Long
            val biggestHealth = getMaxHealthFor(entity)
            if (biggestHealth == 0L) {
                val currentMaxHealth = entity.baseMaxHealth.toLong()
                maxHealth = max(currentMaxHealth, health)
                setMaxHealth(entity, maxHealth)
            } else {
                maxHealth = biggestHealth
            }

            entityData.namePrefix = ""
            entityData.nameSuffix = ""
            entityData.nameAbove = ""
            val customHealthText = if (health == 0L) {
                entityData.dead = true
                "§cDead"
            } else {
                getCustomHealth(entityData, health, entity, maxHealth) ?: return
            }

            if (data.containsKey(entity.uniqueID)) {
                val lastHealth = data[entity.uniqueID]!!.lastHealth
                checkDamage(entityData, health, lastHealth)
                tickDamage(entityData.damageCounter)
                BossHealthChangeEvent(entityData, lastHealth, health, maxHealth).postAndCatch()
            }
            entityData.lastHealth = health

            if (customHealthText.isNotEmpty()) {
                entityData.healthText = customHealthText
            } else {
                val color = NumberUtil.percentageColor(health, maxHealth)
                entityData.healthText = color.getChatColor() + NumberUtil.format(health)
            }
            entityData.timeLastTick = System.currentTimeMillis()
            data[entity.uniqueID] = entityData

        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun getCustomHealth(
        entityData: EntityData,
        health: Long,
        entity: EntityLivingBase,
        maxHealth: Long,
    ): String? {

        when (entityData.bossType) {
            BossType.DUNGEON_F4_THORN -> return checkThorn(health)

            BossType.SLAYER_ENDERMAN_1,
            BossType.SLAYER_ENDERMAN_2,
            BossType.SLAYER_ENDERMAN_3,
            BossType.SLAYER_ENDERMAN_4,
            -> {
                return checkEnderSlayer(entity as EntityEnderman, entityData, health.toInt(), maxHealth.toInt())
            }

            BossType.SLAYER_BLAZE_1,
            BossType.SLAYER_BLAZE_2,
            BossType.SLAYER_BLAZE_3,
            BossType.SLAYER_BLAZE_4,
            BossType.SLAYER_BLAZE_QUAZII_2,
            BossType.SLAYER_BLAZE_QUAZII_3,
            BossType.SLAYER_BLAZE_QUAZII_4,
            BossType.SLAYER_BLAZE_TYPHOEUS_2,
            BossType.SLAYER_BLAZE_TYPHOEUS_3,
            BossType.SLAYER_BLAZE_TYPHOEUS_4,
            -> {
                return checkBlazeSlayer(entity as EntityLiving, entityData, health.toInt(), maxHealth.toInt())
            }

            BossType.NETHER_MAGMA_BOSS -> {
                return checkMagmaCube(entity as EntityMagmaCube, entityData, health.toInt(), maxHealth.toInt())
            }

            BossType.SLAYER_ZOMBIE_5 -> {
                if ((entity as EntityZombie).hasNameTagWith(3, "§fBoom!")) {
                    //TODO fix
//                    val ticksAlive = entity.ticksExisted % (20 * 5)
//                    val remainingTicks = (5 * 20).toLong() - ticksAlive
//                    val format = formatDelay(remainingTicks * 50)
//                    entityData.nameSuffix = " §f§lBOOM - $format"
                    entityData.nameSuffix = " §f§lBOOM!"
                }
            }

            BossType.SLAYER_WOLF_3,
            BossType.SLAYER_WOLF_4,
            -> {
                if ((entity as EntityWolf).hasNameTagWith(2, "§bCalling the pups!")) {
                    return "Pups!"
                }
            }

            BossType.NETHER_BARBARIAN_DUKE,
            -> {
                val location = entity.getLorenzVec()
                entityData.ignoreBlocks = location.y == 117.0 && location.distance(LocationUtils.playerLocation()) < 15
            }

            else -> return ""
        }
        return ""
    }

    private fun checkBlazeSlayer(entity: EntityLiving, entityData: EntityData, health: Int, maxHealth: Int): String? {
        val shields = HellionShield.values()
        var found = false
        for (shield in shields) {
            val armorStand = entity.getNameTagWith(3, shield.name)
            if (armorStand != null) {
                val number = armorStand.name.split(" ♨")[1].substring(0, 1)
                entity.setHellionShield(shield)
                entityData.nameAbove = shield.formattedName + " $number"
                found = true
                break
            }
        }
        if (!found) {
            entity.setHellionShield(null)
        }

        if (!SkyHanniMod.feature.slayer.blazePhaseDisplay) return ""

        var calcHealth = health
        val calcMaxHealth: Int
        entityData.namePrefix = when (entityData.bossType) {
            BossType.SLAYER_BLAZE_1,
            BossType.SLAYER_BLAZE_2,
            -> {
                val step = maxHealth / 2
                calcMaxHealth = step
                if (health > step) {
                    calcHealth -= step
                    "§c1/2 "
                } else {
                    calcHealth = health
                    "§a2/2 "
                }
            }

            BossType.SLAYER_BLAZE_3,
            BossType.SLAYER_BLAZE_4,
            -> {
                val step = maxHealth / 3
                calcMaxHealth = step
                if (health > step * 2) {
                    calcHealth -= step * 2
                    "§c1/3 "
                } else if (health > step) {
                    calcHealth -= step
                    "§e2/3 "
                } else {
                    calcHealth = health
                    "§a3/3 "
                }
            }

            else -> return ""
        }

        return NumberUtil.percentageColor(
            calcHealth.toLong(), calcMaxHealth.toLong()
        ).getChatColor() + NumberUtil.format(calcHealth)
    }

    private fun checkMagmaCube(
        entity: EntityMagmaCube,
        entityData: EntityData,
        health: Int,
        maxHealth: Int,
    ): String? {
        val slimeSize = entity.slimeSize
        entityData.namePrefix = when (slimeSize) {
            24 -> "§c1/6"
            22 -> "§e2/6"
            20 -> "§e3/6"
            18 -> "§e4/6"
            16 -> "§e5/6"
            else -> {
                val color = NumberUtil.percentageColor(health.toLong(), 10_000_000)
                entityData.namePrefix = "§a6/6"
                return color.getChatColor() + NumberUtil.format(health)
            }
        } + " §f"

        //hide while in the middle
//        val position = entity.getLorenzVec()
        //TODO other logic or something
//        entityData.healthLineHidden = position.x == -368.0 && position.z == -804.0

        var calcHealth = -1
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
        if (calcHealth == -1) return null

        val color = NumberUtil.percentageColor(calcHealth.toLong(), maxHealth.toLong())
        return color.getChatColor() + NumberUtil.format(calcHealth)
    }

    private fun checkEnderSlayer(
        entity: EntityEnderman,
        entityData: EntityData,
        health: Int,
        maxHealth: Int,
    ): String? {

        var calcHealth = health
        val calcMaxHealth: Int
        entityData.namePrefix = when (entityData.bossType) {
            BossType.SLAYER_ENDERMAN_1,
            BossType.SLAYER_ENDERMAN_2,
            BossType.SLAYER_ENDERMAN_3,
            -> {
                val step = maxHealth / 3
                calcMaxHealth = step
                if (health > step * 2) {
                    calcHealth -= step * 2
                    "§c1/3 "
                } else if (health > step) {
                    calcHealth -= step
                    "§e2/3 "
                } else {
                    calcHealth = health
                    "§a3/3 "
                }
            }

            BossType.SLAYER_ENDERMAN_4 -> {
                val step = maxHealth / 6
                calcMaxHealth = step
                if (health > step * 5) {
                    calcHealth -= step * 5
                    "§c1/6 "
                } else if (health > step * 4) {
                    calcHealth -= step * 4
                    "§e2/6 "
                } else if (health > step * 3) {
                    calcHealth -= step * 3
                    "§e3/6 "
                } else if (health > step * 2) {
                    calcHealth -= step * 2
                    "§e4/6 "
                } else if (health > step) {
                    calcHealth -= step
                    "§e5/6 "
                } else {
                    calcHealth = health
                    "§a6/6 "
                }
            }

            else -> return null
        }
        var result = NumberUtil.percentageColor(
            calcHealth.toLong(), calcMaxHealth.toLong()
        ).getChatColor() + NumberUtil.format(calcHealth)

        if (!SkyHanniMod.feature.slayer.endermanPhaseDisplay) {
            result = ""
            entityData.namePrefix = ""
        }


        //Hit phase
        val armorStandHits = entity.getNameTagWith(3, " Hit")
        if (armorStandHits != null) {
            val maxHits = when (entityData.bossType) {
                BossType.SLAYER_ENDERMAN_1 -> 15
                BossType.SLAYER_ENDERMAN_2 -> 30
                BossType.SLAYER_ENDERMAN_3 -> 60
                BossType.SLAYER_ENDERMAN_4 -> 100
                else -> 100
            }
            val name = armorStandHits.name.removeColor()
            val hits = name.between("Seraph ", " Hit").toInt()
            return NumberUtil.percentageColor(hits.toLong(), maxHits.toLong()).getChatColor() + "$hits Hits"
        }

        //Laser phase
        if (entity.ridingEntity != null) {
            val ticksAlive = entity.ridingEntity.ticksExisted.toLong()
            //TODO more tests, more exact values, better logic? idk make this working perfectly pls
            //val remainingTicks = 8 * 20 - ticksAlive
            val remainingTicks = (8.9 * 20).toLong() - ticksAlive

            if (SkyHanniMod.feature.damageIndicator.showHealthDuringLaser) {
                entityData.nameSuffix = " §f" + formatDelay(remainingTicks * 50)
            } else {
                return formatDelay(remainingTicks * 50)
            }
        }

        return result
    }

    private fun checkThorn(realHealth: Long): String? {
        val maxHealth: Int
        val health = if (DungeonData.isOneOf("F4")) {
            maxHealth = 4
            when (realHealth.toInt()) {
                300_000, 600_000 -> 4
                222_000, 444_000 -> 3
                144_000, 288_000 -> 2
                66_000, 132_000 -> 1
                0 -> 0
                else -> {
                    LorenzUtils.error(
                        "Unexpected health of thorn in f4! (${
                            LorenzUtils.formatDouble(realHealth.toDouble())
                        })"
                    )
                    return null
                }
            }
        } else if (DungeonData.isOneOf("M4")) {
            maxHealth = 6
            when (realHealth.toInt()) {
                //TODO test all non derpy values!
                1_800_000 / 2, 1_800_000 -> 6
                1_494_000 / 2, 1_494_000 -> 5
                1_188_000 / 2, 1_188_000 -> 4
                882_000 / 2, 882_000 -> 3
                576_000 / 2, 576_000 -> 2
                270_000 / 2, 270_000 -> 1
                0 -> 0
                else -> {
                    LorenzTest.text = "thorn has ${LorenzUtils.formatDouble(realHealth.toDouble())} hp!"
                    LorenzUtils.error(
                        "Unexpected health of thorn in m4! (${
                            LorenzUtils.formatDouble(
                                LorenzUtils.formatDouble(
                                    realHealth.toDouble()
                                ).toDouble()
                            )
                        })"
                    )
                    return null
                }
            }
        } else {
            LorenzUtils.error("Invalid thorn floor!")
            return null
        }
        val color = NumberUtil.percentageColor(health.toLong(), maxHealth.toLong())
        return color.getChatColor() + health + "/" + maxHealth
    }

    private fun checkDamage(entityData: EntityData, health: Long, lastHealth: Long) {
        val damage = lastHealth - health
        val healing = health - lastHealth
        if (damage > 0) {
            if (entityData.bossType != BossType.DUMMY) {
                val damageCounter = entityData.damageCounter
                damageCounter.currentDamage += damage
            }
        }
        if (healing > 0) {
            //Hide auto heal every 10 ticks (with rounding errors)
            if ((healing == 15_000L || healing == 15_001L) && entityData.bossType == BossType.SLAYER_ZOMBIE_5) return

            val damageCounter = entityData.damageCounter
            damageCounter.currentHealing += healing
        }
    }

    private fun grabData(entity: EntityLivingBase): EntityData? {
        if (data.contains(entity.uniqueID)) return data[entity.uniqueID]

        val entityResult = mobFinder?.tryAdd(entity) ?: return null
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

    private fun setMaxHealth(entity: EntityLivingBase, currentMaxHealth: Long) {
        maxHealth[entity.uniqueID!!] = currentMaxHealth
    }

    private fun getMaxHealthFor(entity: EntityLivingBase): Long {
        return maxHealth.getOrDefault(entity.uniqueID!!, 0L)
    }

    @SubscribeEvent
    fun onEntityJoin(event: EntityJoinWorldEvent) {
        mobFinder?.handleNewEntity(event.entity)
    }

    private val dummyDamageCache = mutableListOf<UUID>()

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onRenderLiving(event: RenderLivingEvent.Specials.Pre<EntityLivingBase>) {
        val entity = event.entity

        val entityData = data.values.find {
            val distance = it.entity.getLorenzVec().distance(entity.getLorenzVec())
            distance < 4.5
        }

        if (isDamageSplash(entity)) {
            val name = entity.customNameTag.removeColor().replace(",", "")

            if (SkyHanniMod.feature.misc.fixSkytilsDamageSplash) {
                entity.customNameTag = entity.customNameTag.replace(",", "")
            }
            if (entityData != null) {
                if (SkyHanniMod.feature.damageIndicator.hideDamageSplash) {
                    event.isCanceled = true
                }
                if (entityData.bossType == BossType.DUMMY) {

                    val uuid = entity.uniqueID
                    if (dummyDamageCache.contains(uuid)) return
                    dummyDamageCache.add(uuid)
                    val dmg = name.toCharArray().filter { Character.isDigit(it) }.joinToString("").toLong()
                    entityData.damageCounter.currentDamage += dmg
                }
            }
        } else {
            if (entityData != null) {
                if (isEnabled()) {
                    if (SkyHanniMod.feature.damageIndicator.hideVanillaNametag) {
                        val name = entity.name
                        if (name.contains("Plaesmaflux")) return
                        if (name.contains("Overflux")) return
                        if (name.contains("Mana Flux")) return
                        if (name.contains("Radiant")) return
                        event.isCanceled = true
                    }
                }
            }
        }
    }

    fun isEnabled(): Boolean {
        return LorenzUtils.inSkyBlock && SkyHanniMod.feature.damageIndicator.enabled
    }
}