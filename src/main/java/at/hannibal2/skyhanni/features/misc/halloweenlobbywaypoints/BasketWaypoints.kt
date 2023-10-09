package at.hannibal2.skyhanni.features.misc.halloweenlobbywaypoints

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ScoreboardData
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
    private var isHalloween: Boolean = chechScoreboardHalloweenSpecific()

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!event.repeatSeconds(1)) return
        isHalloween = chechScoreboardHalloweenSpecific()
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!LorenzUtils.onHypixel) return
        if (LorenzUtils.inSkyBlock) return
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
        return (
            ( ScoreboardData.sidebarLinesFormatted.any {
                    it.contains("Hypixel Level")
                    && it.contains("Halloween ")
                    && it.contains("Baskets ")
                }
            )
        )
    }
}