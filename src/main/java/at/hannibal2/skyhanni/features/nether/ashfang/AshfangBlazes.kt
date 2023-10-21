package at.hannibal2.skyhanni.features.nether.ashfang

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.RenderMobColoredEvent
import at.hannibal2.skyhanni.events.ResetEntityHurtEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.features.combat.damageindicator.BossType
import at.hannibal2.skyhanni.features.combat.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.getAllNameTagsWith
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityBlaze
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class AshfangBlazes {
    private val config get() = SkyHanniMod.feature.crimsonIsle.ashfang

    private val blazeColor = mutableMapOf<EntityBlaze, LorenzColor>()
    private val blazeArmorStand = mutableMapOf<EntityBlaze, EntityArmorStand>()

    var nearAshfang = false

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return

        if (event.repeatSeconds(1)) {
            checkNearAshfang()
        }

        if (nearAshfang) {
            for (entity in EntityUtils.getEntities<EntityBlaze>()
                .filter { it !in blazeColor.keys }) {
                val list = entity.getAllNameTagsWith(2, "Ashfang")
                if (list.size == 1) {
                    val armorStand = list[0]
                    blazeArmorStand[entity] = armorStand
                    val color = when {
                        armorStand.name.contains("Ashfang Follower") -> LorenzColor.DARK_GRAY
                        armorStand.name.contains("Ashfang Underling") -> LorenzColor.RED
                        armorStand.name.contains("Ashfang Acolyte") -> LorenzColor.BLUE
                        else -> {
                            blazeArmorStand.remove(entity)
                            null
                        }
                    }
                    color?.let {
                        blazeColor[entity] = it
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onEntityHealthUpdate(event: EntityHealthUpdateEvent) {
        if (!isEnabled()) return

        val entityId = event.entity.entityId
        if (entityId !in blazeArmorStand.keys.map { it.entityId }) return

        if (event.health % 10_000_000 != 0) {
            blazeArmorStand.keys.removeIf { it.entityId == entityId }
        }
    }

    private fun checkNearAshfang() {
        nearAshfang = EntityUtils.getEntities<EntityArmorStand>().any { it.name.contains("Ashfang") }
    }

    @SubscribeEvent
    fun onRenderMobColored(event: RenderMobColoredEvent) {
        if (!isEnabled()) return
        if (!config.highlightBlazes) return
        val entity = event.entity
        event.color = blazeColor[entity]?.toColor()?.withAlpha(40) ?: 0
    }

    @SubscribeEvent
    fun onResetEntityHurtTime(event: ResetEntityHurtEvent) {
        if (!isEnabled()) return
        if (!config.highlightBlazes) return
        val entity = event.entity
        if (entity in blazeColor) {
            event.shouldReset = true
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onRenderLiving(event: RenderLivingEvent.Specials.Pre<EntityLivingBase>) {
        if (!isEnabled()) return
        if (!config.hide.fullNames) return

        val entity = event.entity
        if (entity !is EntityArmorStand) return
        if (!entity.hasCustomName()) return
        if (entity.isDead) return
        if (entity in blazeArmorStand.values) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        blazeColor.clear()
        blazeArmorStand.clear()
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "ashfang.nextResetCooldown", "crimsonIsle.ashfang.nextResetCooldown")
        event.move(2, "ashfang.highlightBlazes", "crimsonIsle.ashfang.highlightBlazes")
        event.move(2, "ashfang.hideNames", "crimsonIsle.ashfang.hide.fullNames")
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyBlock && DamageIndicatorManager.isBossSpawned(BossType.NETHER_ASHFANG)
    }
}