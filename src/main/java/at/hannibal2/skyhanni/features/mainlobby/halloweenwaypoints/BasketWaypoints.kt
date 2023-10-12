package at.hannibal2.skyhanni.features.mainlobby.halloweenwaypoints

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.anyContains
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class BasketWaypoints {
    private val config get() = SkyHanniMod.feature.event.halloweenBasket
    private var waypoint: LorenzVec? = null
    private var waypointName: String? = null
    private var isHalloween: Boolean = false

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!config.allWaypoints && !config.allEntranceWaypoints) return
        if (!isHalloween) return

        val message = event.message
        if (message.startsWith("§a§lYou found a Candy Basket! §r") || message == "§cYou already found this Candy Basket!") {
            val basket = Basket.entries.minByOrNull { it.waypoint.distanceSqToPlayer() }!!
            basket.found = true
        }

    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!config.allWaypoints && !config.allEntranceWaypoints) return
        if (!HypixelData.hypixelLive) return // don't show outside live hypixel network (it's disabled on alpha)
        if (LorenzUtils.inSkyBlock) return

        if (event.repeatSeconds(1)) {
            isHalloween = chechScoreboardHalloweenSpecific()
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!isHalloween) return

        if (config.allWaypoints) {
            for (basket in Basket.entries) {
                if (basket.found) continue
                event.drawWaypointFilled(basket.waypoint, LorenzColor.GOLD.toColor())
                event.drawDynamicText(basket.waypoint, "§6" + basket.basketName, 1.5)
            }
        }

        if (config.allEntranceWaypoints) {
            for (basketEntrance in BasketEntrances.entries) {
                if (basketEntrance.basket.found) continue
                event.drawWaypointFilled(basketEntrance.waypoint, LorenzColor.YELLOW.toColor())
                event.drawDynamicText(basketEntrance.waypoint, "§e" + basketEntrance.basketEntranceName, 1.5)
            }
            return
        }

        if (LorenzUtils.skyBlockArea == "?") return

        waypoint?.let {
            event.drawWaypointFilled(it, LorenzColor.GOLD.toColor())
            event.drawDynamicText(it, "§6" + waypointName!!, 1.5)
        }
    }

    private fun chechScoreboardHalloweenSpecific(): Boolean {
        val list = ScoreboardData.sidebarLinesFormatted
        return list.anyContains("Hypixel Level") && list.anyContains("Halloween") && list.anyContains("Baskets")
    }
}