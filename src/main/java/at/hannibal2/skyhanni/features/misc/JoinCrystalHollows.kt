package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class JoinCrystalHollows {

    private var lastWrongPassTime = 0L

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return

        val message = event.message
        if (message == "§cYou do not have an active Crystal Hollows pass!") {
            lastWrongPassTime = System.currentTimeMillis()
            if (LorenzUtils.skyBlockIsland != IslandType.DWARVEN_MINES) {
                LorenzUtils.clickableChat("§e[SkyHanni] Click here to warp to Dwarven Mines!", "warp mines")
            } else {
                LorenzUtils.chat("Buy a §2Crystal Hollows Pass §efrom §5Gwendolyn")
            }
        }
        if (message == "§e[NPC] §5Gwendolyn§f: §rGreat! Now hop on into the Minecart and I'll get you on your way!" && inTime()) {
            LorenzUtils.clickableChat("§e[SkyHanni] Click here to warp to Crystal Hollows!", "warp ch")
        }
    }

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (!isEnabled()) return

        if (event.newIsland == IslandType.DWARVEN_MINES && inTime()) {
                LorenzUtils.chat("Buy a §2Crystal Hollows Pass §efrom §5Gwendolyn§e!")
        }
        if (event.newIsland == IslandType.CRYSTAL_HOLLOWS) {
            lastWrongPassTime = 0
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!isEnabled()) return
        if (LorenzUtils.skyBlockIsland != IslandType.DWARVEN_MINES) return

        if (inTime()) {
            val location = LorenzVec(88, 198, -99)
            event.drawWaypointFilled(location, LorenzColor.YELLOW.toColor())
            event.drawDynamicText(location, "§eBuy Crystal Hollows Pass", 1.3)
        }
    }

    private fun inTime() = lastWrongPassTime + 1000 * 60 * 2 > System.currentTimeMillis()

    fun isEnabled() = SkyHanniMod.feature.misc.crystalHollowsJoin
}
