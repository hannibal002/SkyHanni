package at.hannibal2.skyhanni.features.slayer.enderman

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.RenderMobColoredEvent
import at.hannibal2.skyhanni.events.ServerBlockChangeEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.EntityUtils.getBlockInHand
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.LorenzUtils.toChromaColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.RenderUtils.drawColor
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.exactLocation
import at.hannibal2.skyhanni.utils.RenderUtils.exactPlayerEyeLocation
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.init.Blocks
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class EndermanSlayerFeatures {
    private val config get() = SkyHanniMod.feature.slayer.endermen
    private val beaconConfig get() = config.endermanBeaconConfig
    private val endermenWithBeacons = mutableListOf<EntityEnderman>()
    private var flyingBeacons = listOf<EntityArmorStand>()
    private val nukekubiSkulls = mutableListOf<EntityArmorStand>()
    private var sittingBeacon = mapOf<LorenzVec, SimpleTimeMark>()
    private val logger = LorenzLogger("slayer/enderman")
    private val nukekubiSkulTexture =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWIwNzU5NGUyZGYyNzM5MjFhNzdjMTAxZDBiZmRmYTExMTVhYmVkNWI5YjIwMjllYjQ5NmNlYmE5YmRiYjRiMyJ9fX0="

    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!IslandType.THE_END.isInIsland()) return
        val entity = event.entity
        if (entity in endermenWithBeacons || entity in flyingBeacons) return

        if (entity is EntityEnderman && showBeacon() && hasBeaconInHand(entity) && canSee(
                LocationUtils.playerEyeLocation(),
                entity.getLorenzVec()
            )
        ) {
            endermenWithBeacons.add(entity)
            logger.log("Added enderman with beacon at ${entity.getLorenzVec()}")
        }

        if (entity is EntityArmorStand) {
            if (showBeacon()) {
                val stack = entity.inventory[4] ?: return
                if (stack.name == "Beacon" && canSee(LocationUtils.playerEyeLocation(), entity.getLorenzVec())) {
                    flyingBeacons = flyingBeacons.editCopy {
                        add(entity)
                    }
                    if (beaconConfig.showWarning) {
                        LorenzUtils.sendTitle("§4Beacon", 2.seconds)
                    }
                    logger.log("Added flying beacons at ${entity.getLorenzVec()}")
                }
            }

            if (config.highlightNukekebi && entity.inventory.any { it?.getSkullTexture() == nukekubiSkulTexture } && entity !in nukekubiSkulls) {
                nukekubiSkulls.add(entity)
                logger.log("Added Nukekubi skulls at ${entity.getLorenzVec()}")
            }
        }
    }

    private fun hasBeaconInHand(enderman: EntityEnderman) = enderman.getBlockInHand()?.block == Blocks.beacon

    private fun canSee(a: LorenzVec, b: LorenzVec) = LocationUtils.canSee(a, b) || a.distance(b) < 15

    private fun showBeacon() = beaconConfig.highlightBeacon || beaconConfig.showWarning || beaconConfig.showLine

    @SubscribeEvent
    fun onRenderMobColored(event: RenderMobColoredEvent) {
        if (!IslandType.THE_END.isInIsland()) return

        if (beaconConfig.highlightBeacon && event.entity in flyingBeacons) {
            event.color = beaconConfig.beaconColor.toChromaColor().withAlpha(1)
        }

        if (config.highlightNukekebi && event.entity in nukekubiSkulls) {
            event.color = LorenzColor.GOLD.toColor().withAlpha(1)
        }
    }

    @SubscribeEvent
    fun onWorldRender(event: LorenzRenderWorldEvent) {
        if (!IslandType.THE_END.isInIsland()) return


        if (beaconConfig.highlightBeacon) {
            endermenWithBeacons.removeIf { it.isDead || !hasBeaconInHand(it) }

            endermenWithBeacons.map { it.getLorenzVec().add(-0.5, 0.2, -0.5) }
                .forEach { event.drawColor(it, beaconConfig.beaconColor.toChromaColor(), alpha = 0.5f) }
        }

        for ((location, time) in sittingBeacon) {
            if (location.distanceToPlayer() > 20) continue
            if (beaconConfig.showLine) {
                event.draw3DLine(
                    event.exactPlayerEyeLocation(),
                    location.add(0.5, 1.0, 0.5),
                    beaconConfig.lineColor.toChromaColor(),
                    beaconConfig.lineWidth,
                    true
                )
            }

            if (beaconConfig.highlightBeacon) {
                val duration = 5.seconds - time.passedSince()
                val durationFormat = duration.format(showMilliSeconds = true)
                event.drawColor(location, beaconConfig.beaconColor.toChromaColor(), alpha = 1f)
                event.drawWaypointFilled(location, beaconConfig.beaconColor.toChromaColor(), true, true)
                event.drawDynamicText(location.add(0, 1, 0), "§4Beacon §b$durationFormat", 1.8)
            }
        }
        for (beacon in flyingBeacons) {
            if (beacon.isDead) continue
            if (beaconConfig.highlightBeacon) {
                val beaconLocation = event.exactLocation(beacon)
                event.drawDynamicText(beaconLocation.add(0, 1, 0), "§4Beacon", 1.8)
            }

            if (beaconConfig.showLine) {
                val beaconLocation = event.exactLocation(beacon)
                event.draw3DLine(
                    event.exactPlayerEyeLocation(),
                    beaconLocation.add(0.5, 1.0, 0.5),
                    beaconConfig.lineColor.toChromaColor(),
                    beaconConfig.lineWidth,
                    true
                )
            }
        }

        config.highlightNukekebi
        for (skull in nukekubiSkulls) {
            if (!skull.isDead) {
                event.drawDynamicText(
                    skull.getLorenzVec().add(-0.5, 1.5, -0.5),
                    "§6Nukekubi Skull",
                    1.6,
                    ignoreBlocks = false
                )
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!IslandType.THE_END.isInIsland()) return
        if (!event.repeatSeconds(1)) return

        nukekubiSkulls.also { skulls -> skulls.removeAll { it.isDead } }
        if (flyingBeacons.any { it.isDead }) {
            flyingBeacons = flyingBeacons.editCopy {
                removeAll { it.isDead }
            }
        }

        // Removing the beacon if It's still there after 7 sesconds.
        // This is just a workaround for the cases where the ServerBlockChangeEvent don't detect the beacon despawn info.
        val toRemove = sittingBeacon.filter { it.value.passedSince() > 7.seconds }
        if (toRemove.isNotEmpty()) {
            sittingBeacon = sittingBeacon.editCopy {
                toRemove.keys.forEach { remove(it) }
            }
        }
    }

    @SubscribeEvent
    fun onBlockChange(event: ServerBlockChangeEvent) {
        if (!IslandType.THE_END.isInIsland()) return
        if (!showBeacon()) return

        val location = event.location
        if (event.new == "beacon") {
            val armorStand = flyingBeacons.find { location.distance(it.getLorenzVec()) < 3 }
            if (armorStand != null) {
                flyingBeacons = flyingBeacons.editCopy {
                    remove(armorStand)
                }
                sittingBeacon = sittingBeacon.editCopy { this[location] = SimpleTimeMark.now() }
                logger.log("Replaced flying beacon with sitting beacon at $location")
            }
        } else {
            if (location in sittingBeacon) {
                logger.log("Removed sitting beacon $location")
                sittingBeacon = sittingBeacon.editCopy { remove(location) }
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        endermenWithBeacons.clear()
        flyingBeacons = emptyList()
        nukekubiSkulls.clear()
        sittingBeacon = emptyMap()
        logger.log("Reset everything (world change)")
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(
            3,
            "slayer.endermanBeaconConfig.highlightBeacon",
            "slayer.endermen.endermanBeaconConfig.highlightBeacon"
        )
        event.move(3, "slayer.endermanBeaconConfig.beaconColor", "slayer.endermen.endermanBeaconConfig.beaconColor")
        event.move(3, "slayer.endermanBeaconConfig.showWarning", "slayer.endermen.endermanBeaconConfig.showWarning")
        event.move(3, "slayer.endermanBeaconConfig.showLine", "slayer.endermen.endermanBeaconConfig.showLine")
        event.move(3, "slayer.endermanBeaconConfig.lneColor", "slayer.endermen.endermanBeaconConfig.lineColor")
        event.move(3, "slayer.endermanBeaconConfig.lineWidth", "slayer.endermen.endermanBeaconConfig.lineWidth")
        event.move(3, "slayer.endermanHighlightNukekebi", "slayer.endermen.highlightNukekebi")
    }
}
