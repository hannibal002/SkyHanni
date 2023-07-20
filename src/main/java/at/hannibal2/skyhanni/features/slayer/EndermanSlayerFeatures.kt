package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.TitleUtils
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.RenderMobColoredEvent
import at.hannibal2.skyhanni.events.ServerBlockChangeEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.EntityUtils.getBlockInHand
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.RenderUtils.drawColor
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.init.Blocks
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class EndermanSlayerFeatures {
    private val config get() = SkyHanniMod.feature.slayer
    private val endermenWithBeacons = mutableListOf<EntityEnderman>()
    private val flyingBeacons = mutableListOf<EntityArmorStand>()
    private val nukekebiSkulls = mutableListOf<EntityArmorStand>()
    private val sittingBeacon = mutableListOf<LorenzVec>()
    private val logger = LorenzLogger("slayer/enderman")
    private val nukekebiSkulTexture =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWIwNzU5NGUyZGYyNzM5MjFhNzdjMTAxZDBiZmRmYTExMTVhYmVkNWI5YjIwMjllYjQ5NmNlYmE5YmRiYjRiMyJ9fX0="

    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!IslandType.THE_END.isInIsland()) return
        val entity = event.entity
        if (entity in endermenWithBeacons || entity in flyingBeacons) return

        if (entity is EntityEnderman) {
            if (config.slayerEndermanBeacon) {
                if (hasBeaconInHand(entity) && canSee(LocationUtils.playerEyeLocation(), entity.getLorenzVec())) {
                    endermenWithBeacons.add(entity)
                    logger.log("Added enderman with beacon at ${entity.getLorenzVec()}")
                }
            }
        }

        if (entity is EntityArmorStand) {
            if (config.slayerEndermanBeacon) {
                val stack = entity.inventory[4] ?: return
                if (stack.name == "Beacon" && canSee(LocationUtils.playerEyeLocation(), entity.getLorenzVec())) {
                    flyingBeacons.add(entity)
                    if (config.slayerEndermanBeaconWaring)
                        TitleUtils.sendTitle("§4Beacon", 2_00)
                    logger.log("Added flying beacons at ${entity.getLorenzVec()}")
                }
            }

            if (config.endermanHighlightNukekebi) {
                if (entity.inventory.any { it?.getSkullTexture() == nukekebiSkulTexture }) {
                    nukekebiSkulls.add(entity)
                    logger.log("Added nukekebi skulls at ${entity.getLorenzVec()}")
                    nukekebiSkulls.also { it.removeAll { it.isDead } }
                }
            }
        }
    }

    private fun hasBeaconInHand(enderman: EntityEnderman) = enderman.getBlockInHand()?.block == Blocks.beacon

    private fun canSee(a: LorenzVec, b: LorenzVec) = LocationUtils.canSee(a, b) || a.distance(b) < 15

    @SubscribeEvent
    fun onRenderMobColored(event: RenderMobColoredEvent) {
        if (!IslandType.THE_END.isInIsland()) return

        if (config.slayerEndermanBeacon && event.entity in flyingBeacons) {
            event.color = LorenzColor.DARK_RED.toColor().withAlpha(1)
        }

        if (config.endermanHighlightNukekebi && event.entity in nukekebiSkulls) {
            event.color = LorenzColor.GOLD.toColor().withAlpha(1)
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onWorldRender(event: RenderWorldLastEvent) {
        if (!IslandType.THE_END.isInIsland()) return
        if (!config.slayerEndermanBeacon) return

        endermenWithBeacons.removeIf { it.isDead || !hasBeaconInHand(it) }

        endermenWithBeacons.map { it.getLorenzVec().add(-0.5, 0.2, -0.5) }
            .forEach { event.drawColor(it, LorenzColor.DARK_RED, alpha = 1f) }

        for (location in sittingBeacon.toMutableList()) {
            event.drawColor(location, LorenzColor.DARK_RED, alpha = 1f)
            event.drawWaypointFilled(location, LorenzColor.RED.toColor(), true, true)
            event.drawString(location.add(0.5, 0.5, 0.5), "§4Beacon", true)

        }
    }

    @SubscribeEvent
    fun onBlockChange(event: ServerBlockChangeEvent) {
        if (!IslandType.THE_END.isInIsland()) return
        if (!config.slayerEndermanBeacon) return

        val location = event.location
        if (event.new == "beacon") {
            val armorStand = flyingBeacons.find { location.distance(it.getLorenzVec()) < 3 }
            if (armorStand != null) {
                flyingBeacons.remove(armorStand)
                sittingBeacon.add(location)
                logger.log("Replaced flying beacon with sitting beacon at $location")
            }
        } else {
            if (location in sittingBeacon) {
                logger.log("Removed sitting beacon $location")
                sittingBeacon.remove(location)
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        endermenWithBeacons.clear()
        flyingBeacons.clear()
        nukekebiSkulls.clear()
        sittingBeacon.clear()
        logger.log("Reset everything (world change)")
    }
}