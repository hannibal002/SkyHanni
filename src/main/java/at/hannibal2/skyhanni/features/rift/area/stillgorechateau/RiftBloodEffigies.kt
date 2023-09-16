package at.hannibal2.skyhanni.features.rift.area.stillgorechateau

import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.ScoreboardRawChangeEvent
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.jsonobjects.RiftEffigiesJson
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class RiftBloodEffigies {
    private val config get() = RiftAPI.config.area.stillgoreChateauConfig.bloodEffigies
    private var locations: List<LorenzVec> = emptyList()
    private var effigiesTimes = mapOf(
        0 to -1L,
        1 to -1L,
        2 to -1L,
        3 to -1L,
        4 to -1L,
        5 to -1L,
    )

    private val effigiesTimerPattern = "§eRespawn §c(?<time>.*) §7\\(or click!\\)".toPattern()

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        effigiesTimes = mapOf(
            0 to -1L,
            1 to -1L,
            2 to -1L,
            3 to -1L,
            4 to -1L,
            5 to -1L,
        )
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        event.getConstant<RiftEffigiesJson>("RiftEffigies")?.locations?.let {
            if (it.size != 6) {
                error("Invalid rift effigies size: ${it.size} (expeced 6)")
            }
            locations = it
        }
    }

    @SubscribeEvent
    fun onScoreboardChange(event: ScoreboardRawChangeEvent) {
        if (!isEnabled()) return

        val line = event.newList.firstOrNull { it.startsWith("Effigies:") } ?: return
        val hearts = "Effigies: (?<hearts>.*)".toPattern().matchMatcher(line) {
            group("hearts")
        } ?: return

        val split = hearts.split("§").drop(1)
        for ((index, s) in split.withIndex()) {
            val time = effigiesTimes[index]!!
            val diff = time - System.currentTimeMillis()
            if (diff < 0L) {
                if (s == "7") {
                    if (time != 0L) {
                        LorenzUtils.chat("§e[SkyHanni] Effigy #${index + 1} respawned!")
                        effigiesTimes = effigiesTimes.editCopy { this[index] = 0L }
                    }
                } else {
                    if (time != -1L) {
                        LorenzUtils.chat("§e[SkyHanni] Effigy #${index + 1} is broken!")
                        val endTime = System.currentTimeMillis() + 1_000 * 60 * 20
                        effigiesTimes = effigiesTimes.editCopy { this[index] = endTime }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!event.repeatSeconds(1)) return
        if (!isEnabled()) return

        for (entity in EntityUtils.getEntitiesNearby<EntityArmorStand>(LocationUtils.playerLocation(), 6.0)) {
            effigiesTimerPattern.matchMatcher(entity.name) {
                val nearest = locations.sortedBy { it.distanceSq(entity.getLorenzVec()) }.firstOrNull() ?: return
                val index = locations.indexOf(nearest)

                val string = group("time")
                val time = TimeUtils.getMillis(string)
                effigiesTimes = effigiesTimes.editCopy { this[index] = System.currentTimeMillis() + time }
            }
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!isEnabled()) return

        for ((index, location) in locations.withIndex()) {
            val name = "Effigy #${index + 1}"
            val duration = effigiesTimes[index]!!

            if (duration == -1L) {
                if (config.unknownTime) {
                    event.drawWaypointFilled(location, LorenzColor.GRAY.toColor(), seeThroughBlocks = true)
                    event.drawDynamicText(location, "§7Unknown Time ($name)", 1.5)
                    continue
                }
            } else {
                val diff = duration - System.currentTimeMillis()
                if (duration <= 0L) {
                    event.drawWaypointFilled(location, LorenzColor.RED.toColor(), seeThroughBlocks = true)
                    event.drawDynamicText(location, "§cBreak $name!", 1.5)
                    continue
                }

                if (config.respawningSoon) {
                    if (diff < 60_000 * config.respwningSoonTime) {
                        val time = TimeUtils.formatDuration(diff - 999)
                        event.drawWaypointFilled(location, LorenzColor.YELLOW.toColor(), seeThroughBlocks = true)
                        event.drawDynamicText(location, "§e$name is respawning §b$time", 1.5)
                        continue
                    }
                }
            }

            if (location.distanceToPlayer() < 5) {
                event.drawDynamicText(location, "§7$name", 1.5)
            }
        }
    }

    fun isEnabled() = RiftAPI.inRift() && RiftAPI.inStillgoreChateau() && config.enabled
}
