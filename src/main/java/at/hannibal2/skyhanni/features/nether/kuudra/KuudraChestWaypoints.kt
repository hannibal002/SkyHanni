package at.hannibal2.skyhanni.features.nether.kuudra

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LocationUtils.canSee
import at.hannibal2.skyhanni.utils.RenderUtils.drawColor
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import net.minecraft.entity.monster.EntityGiantZombie
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class KuudraChestWaypoints {

    private val chests = mutableListOf<EntityGiantZombie>()
    private val logger = LorenzLogger("nether/kuudra")
    private val ChestSkulTexture =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDVjNmRjMmJiZjUxYzM2Y2ZjNzcxNDU4NWE2YTU2ODNlZjJiMTRkNDdkOGZmNzE0NjU0YTg5M2Y1ZGE2MjIifX19"

    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!LorenzUtils.inKuudraFight) return
        if (!SkyHanniMod.feature.kuudra.chestWaypoints) return
        val entity = event.entity
        if (entity in chests) return

        chests.removeIf { it.isDead }

        if (entity is EntityGiantZombie) {
            if (entity.inventory.any { it.getSkullTexture() == ChestSkulTexture } && canSee(
                    LocationUtils.playerEyeLocation(),
                    entity.getLorenzVec()
                )) {
                chests.add(entity)
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onWorldRender(event: RenderWorldLastEvent) {
        if (!LorenzUtils.inKuudraFight) return
        if (!SkyHanniMod.feature.kuudra.chestWaypoints) return

        for (chest in chests.toMutableList()) {
            val location = chest.position.toLorenzVec()
            event.drawColor(location, LorenzColor.GOLD, alpha = 1f)
            event.drawWaypointFilled(location, LorenzColor.YELLOW.toColor(), true, true)
            event.drawString(location.add(0.5, 0.5, 0.5), "§eChest", true)
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        chests.clear()
        logger.log("Reset everything (world change)")
    }

}