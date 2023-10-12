package at.hannibal2.skyhanni.features.misc.halloweenlobbywaypoints

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import net.minecraft.client.Minecraft
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class BasketWaypoints {
    private val config get() = SkyHanniMod.feature.misc.halloweenBasket
    private var waypoint: LorenzVec? = null
    private var waypointName: String? = null
    private var isHalloween: Boolean = false

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!config.allWaypoints && !config.allEntranceWaypoints) return
        if (!HypixelData.hypixelLive) return //dont show outside of live hypixel network
        if (LorenzUtils.inSkyBlock) return 
        if (!event.repeatSeconds(1)) return
        isHalloween = chechScoreboardHalloweenSpecific()
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!isHalloween) return

        if (config.allWaypoints) {
            for (basket in Basket.entries) {
                event.drawWaypointFilled(basket.waypoint, LorenzColor.GOLD.toColor())
                event.drawDynamicText(basket.waypoint, "§6" + basket.basketName, 1.5)
            }
        }

        if (config.allEntranceWaypoints) {
            for (basketEntrance in BasketEntrances.entries) {
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
        //am checking separate lines of scoreboard, cannot do `it.contains("xyz") && it.contains("ABC")`
        val theScoreboardList = ScoreboardData.sidebarLinesFormatted
        return ( theScoreboardList.any {
                it.contains("Hypixel Level")
            } && theScoreboardList.any {
                it.contains("Halloween")
            } && theScoreboardList.any {
                it.contains("Baskets")
            }
        )
    }
}