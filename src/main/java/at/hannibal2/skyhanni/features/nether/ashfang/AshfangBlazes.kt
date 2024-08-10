package at.hannibal2.skyhanni.features.nether.ashfang

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.SkyHanniRenderEntityEvent
import at.hannibal2.skyhanni.features.combat.damageindicator.BossType
import at.hannibal2.skyhanni.features.combat.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.CollectionUtils.editCopy
import at.hannibal2.skyhanni.utils.ColorUtils.withAlpha
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.getAllNameTagsWith
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityBlaze
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object AshfangBlazes {

    private val config get() = SkyHanniMod.feature.crimsonIsle.ashfang

    private val blazeColor = mutableMapOf<EntityBlaze, LorenzColor>()
    private var blazeArmorStand = mapOf<EntityBlaze, EntityArmorStand>()

    private var nearAshfang = false

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return

        checkNearAshfang()

        if (nearAshfang) {
            for (entity in EntityUtils.getEntities<EntityBlaze>()
                .filter { it !in blazeColor.keys }) {
                val list = entity.getAllNameTagsWith(2, "Ashfang")
                if (list.size == 1) {
                    val armorStand = list[0]
                    val color = when {
                        armorStand.name.contains("Ashfang Follower") -> LorenzColor.DARK_GRAY
                        armorStand.name.contains("Ashfang Underling") -> LorenzColor.RED
                        armorStand.name.contains("Ashfang Acolyte") -> LorenzColor.BLUE
                        else -> {
                            blazeArmorStand = blazeArmorStand.editCopy {
                                remove(entity)
                            }
                            continue
                        }
                    }
                    blazeArmorStand = blazeArmorStand.editCopy {
                        this[entity] = armorStand
                    }
                    entity setBlazeColor color
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
            blazeArmorStand = blazeArmorStand.editCopy {
                keys.removeIf { it.entityId == entityId }
            }
        }
    }

    private fun checkNearAshfang() {
        nearAshfang = EntityUtils.getEntities<EntityArmorStand>().any { it.name.contains("Ashfang") }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onRenderLiving(event: SkyHanniRenderEntityEvent.Specials.Pre<EntityLivingBase>) {
        if (!isEnabled()) return
        if (!config.hide.fullNames) return

        val entity = event.entity
        if (entity !is EntityArmorStand) return
        if (!entity.hasCustomName()) return
        if (entity.isDead) return
        if (entity in blazeArmorStand.values) {
            event.cancel()
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        blazeColor.clear()
        blazeArmorStand = emptyMap()
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "ashfang.nextResetCooldown", "crimsonIsle.ashfang.nextResetCooldown")
        event.move(2, "ashfang.highlightBlazes", "crimsonIsle.ashfang.highlightBlazes")
        event.move(2, "ashfang.hideNames", "crimsonIsle.ashfang.hide.fullNames")
    }

    private fun isEnabled(): Boolean {
        return IslandType.CRIMSON_ISLE.isInIsland() && DamageIndicatorManager.isBossSpawned(BossType.NETHER_ASHFANG)
    }

    private infix fun EntityBlaze.setBlazeColor(color: LorenzColor) {
        blazeColor[this] = color
        RenderLivingEntityHelper.setEntityColorWithNoHurtTime(
            this,
            color.toColor().withAlpha(40),
        ) { isEnabled() && config.highlightBlazes }
    }
}
