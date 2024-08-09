package at.hannibal2.skyhanni.features.rift.area.colosseum

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.mob.MobFilter.isSkyBlockMob
import at.hannibal2.skyhanni.events.EntityMaxHealthUpdateEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.entity.EntityHurtEvent
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.StringUtils.pluralize
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.monster.EntitySlime
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.math.ceil

@SkyHanniModule
object TentacleWaypoint {

    private val config get() = SkyHanniMod.feature.rift.area.colosseum
    private var tentacles: MutableMap<EntityLivingBase, Int> = mutableMapOf()

    @SubscribeEvent
    fun onMobSpawn(event: EntityMaxHealthUpdateEvent) {
        if (!isEnabled()) return
        if (event.entity !is EntitySlime) return
        if (!event.entity.isSkyBlockMob()) return
        if (event.entity.displayName.formattedText != "Slime§r") return
        // Only get the tentacle on the ground
        if (ceil(event.entity.posY).toInt() != 68) return
        // Only get the tentacle with size 4 to 8
        if (event.entity.slimeSize !in 4..8) return
        if (tentacles.containsKey(event.entity)) return

        tentacles += event.entity to 0
    }

    @HandleEvent
    fun onEntityDamage(event: EntityHurtEvent) {
        if (!isEnabled()) return
        if (event.entity !is EntitySlime) return
        if (event.entity !in tentacles) return

        val tentacle = tentacles[event.entity] ?: return
        tentacles[event.entity] = tentacle + 1
    }

    @SubscribeEvent
    fun onRender(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        tentacles = tentacles.filterNot { it.key.isDead || it.key.health == 0f }.toMutableMap()

        for (tentacle in tentacles) {
            event.drawWaypointFilled(
                tentacle.key.getLorenzVec().add(-0.5, 0.0, -0.5),
                Color.RED,
                seeThroughBlocks = true,
                beacon = true,
            )
            event.drawDynamicText(
                tentacle.key.getLorenzVec().up(1.0),
                "§a${pluralize(tentacle.value, "Hit", withNumber = true)}",
                1.0,
            )
        }
    }

    fun isEnabled() = RiftAPI.inRift() && RiftAPI.inColosseum() && config.tentacleWaypoints

}
