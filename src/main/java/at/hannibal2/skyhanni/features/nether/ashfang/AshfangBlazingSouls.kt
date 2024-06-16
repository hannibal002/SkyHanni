package at.hannibal2.skyhanni.features.nether.ashfang

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.entity.EntityEnterWorldEvent
import at.hannibal2.skyhanni.features.combat.damageindicator.BossType
import at.hannibal2.skyhanni.features.combat.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.EntityUtils.hasSkullTexture
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.RenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object AshfangBlazingSouls {

    private val config get() = SkyHanniMod.feature.crimsonIsle.ashfang.blazingSouls

    private const val TEXTURE =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODI4N2IzOTdkYWY5NTE2YTBiZDc2ZjVmMWI3YmY5Nzk1MTVkZjNkNWQ4MzNlMDYzNWZhNjhiMzdlZTA4MjIxMiJ9fX0="
    private val souls = mutableSetOf<EntityArmorStand>()


    @HandleEvent(onlyOnSkyblock = true, onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onEntityJoin(event: EntityEnterWorldEvent<EntityArmorStand>) {
        if (!AshfangBlazes.isAshfangActive()) return
        val entity = event.entity
        if (!entity.hasSkullTexture(TEXTURE)) return
        souls += entity
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return

        val color = config.color.toChromaColor()

        val playerLocation = LocationUtils.playerLocation()
        for (orb in souls) {
            if (orb.isDead) continue
            val orbLocation = orb.getLorenzVec()
            event.drawWaypointFilled(orbLocation.add(-0.5, 1.25, -0.5), color, extraSize = -0.15)
            if (orbLocation.distance(playerLocation) < 10) {
                // TODO find way to dynamically change color
                event.drawString(orbLocation.add(y = 2.5), "§bBlazing Soul")
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        souls.clear()
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "ashfang.blazingSouls", "crimsonIsle.ashfang.blazingSouls.enabled")
        event.move(2, "ashfang.blazingSoulsColor", "crimsonIsle.ashfang.blazingSouls.color")
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled &&
        DamageIndicatorManager.isBossSpawned(BossType.NETHER_ASHFANG)
}
