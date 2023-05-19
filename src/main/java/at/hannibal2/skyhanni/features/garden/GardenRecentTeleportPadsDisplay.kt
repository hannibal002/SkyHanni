package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils.onToggle
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenRecentTeleportPadsDisplay {
    private val config get() = SkyHanniMod.feature.garden.teleportPadsRecentDisplay
    private var display = listOf<String>()
    private var recentTeleports = mutableListOf<Pair<String, String>>()
    private val pattern =
        "§aWarped from the §r(?<from>.*) Teleport Pad§r§a to the §r(?<to>.*) Teleport Pad§r§a!".toPattern()

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!GardenAPI.inGarden()) return

        pattern.matchMatcher(event.message) {
            if (config.enabled) {
                recentTeleports.add(Pair(group("from"), group("to")))
                if (recentTeleports.size > 3) recentTeleports.removeFirst()
                update()
            }

            if (config.hideChat) {
                event.blockedReason = "recent_teleport_pads"
            }
        }
    }

    private fun update() {
        display = buildList {
            add("§6Recent TPs")
            for ((from, to) in recentTeleports) {
                if (config.onlyTarget.get()) {
                    add(to)
                } else {
                    add(" $from §7➜ $to")
                }
            }
        }
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        config.onlyTarget.onToggle {
            update()
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return

        config.pos.renderStrings(display, posLabel = "Recent Teleport Pads Display")
    }

    fun isEnabled() = GardenAPI.inGarden() && config.enabled
}
