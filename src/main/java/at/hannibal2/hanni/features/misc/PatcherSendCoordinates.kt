package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.events.minecraft.HanniTickEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ColorUtils.toColor
import at.hannibal2.hanni.utils.LocationUtils
import at.hannibal2.hanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.LorenzLogger
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.StringUtils.removeColor
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawColor
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawString
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object PatcherSendCoordinates {

    private val config get() = HanniMod.feature.misc.patcherCoordsWaypoint
    private val patcherBeacon = mutableListOf<PatcherBeacon>()
    private val logger = LorenzLogger("misc/patchercoords")

    /**
     * REGEX-TEST: hannibal2: x: 2, y: 3, z: 4
     * REGEX-TEST: hannibal2: x: 2, y: 3, z: 4broken
     * REGEX-TEST: hannibal2: x: 2, y: 3, z: 4 extra text
     */
    private val coordinatePattern by RepoPattern.pattern(
        "misc.patchercoords.coords",
        "(?<playerName>.*): [xX]: (?<x>[0-9.-]+),? [yY]: (?<y>[0-9.-]+),? [zZ]: (?<z>[0-9.-]+(?: .*)?)",
    )

    @HandleEvent
    fun onChat(event: HanniChatEvent) {
        if (!config.enabled) return

        val message = event.message.removeColor()
        coordinatePattern.matchMatcher(message) {
            var description = group("playerName").split(" ").last()
            val x = group("x").toFloat()
            val y = group("y").toFloat()

            val end = group("z")
            val z = if (end.contains(" ")) {
                val split = end.split(" ")
                val extra = split.drop(1).joinToString(" ").take(50)
                description += " $extra"

                split.first().toFloat()
            } else end.toFloat()
            patcherBeacon.add(PatcherBeacon(LorenzVec(x, y, z), description, System.currentTimeMillis() / 1000))
            logger.log("got Patcher coords and username")
        }
    }

    @HandleEvent(priority = HandleEvent.HIGH)
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (!config.enabled) return

        for (beacon in patcherBeacon) {
            val location = beacon.location
            val distance = location.distanceToPlayer()
            val formattedDistance = distance.toInt().addSeparators()

            // TODO add chroma color support via config
            event.drawColor(location, LorenzColor.DARK_GREEN.toChromaColor(), alpha = 1f)
            event.drawWaypointFilled(location, config.color.toColor(), seeThroughBlocks = true, beacon = true)
            event.drawString(location.blockCenter(), beacon.name + " §e[${formattedDistance}m]", true, LorenzColor.DARK_BLUE.toColor())
        }
    }

    @HandleEvent
    fun onTick(event: HanniTickEvent) {
        if (!event.isMod(10)) return

        val location = LocationUtils.playerLocation()
        // removed Patcher beacon!
        patcherBeacon.removeIf { System.currentTimeMillis() / 1000 > it.time + 5 && location.distanceIgnoreY(it.location) < 5 }

        // removed Patcher beacon after time!
        patcherBeacon.removeIf { System.currentTimeMillis() / 1000 > it.time + config.duration }
    }

    @HandleEvent
    fun onWorldChange() {
        patcherBeacon.clear()
        logger.log("Reset everything (world change)")
    }

    data class PatcherBeacon(val location: LorenzVec, val name: String, val time: Long)

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(39, "misc.patcherSendCoordWaypoint", "misc.patcherCoordsWaypoint.enabled")
    }
}
