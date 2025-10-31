package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.IslandChangeEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.HypixelCommands
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawWaypointFilled

@HanniModule
object JoinCrystalHollows {

    private var lastWrongPassTime = 0L
    private val location = LorenzVec(88, 198, -99)

    @HandleEvent
    fun onChat(event: HanniChatEvent) {
        if (!isEnabled()) return

        val message = event.message
        if (message == "§cYou do not have an active Crystal Hollows pass!") {
            lastWrongPassTime = System.currentTimeMillis()
            if (!IslandType.DWARVEN_MINES.isCurrent()) {
                ChatUtils.clickableChat(
                    "Click here to warp to Dwarven Mines!",
                    onClick = { HypixelCommands.warp("mines") },
                    hover = "§eClick to run /warp mines!",
                )
            } else {
                ChatUtils.chat("Buy a §2Crystal Hollows Pass §efrom §5Gwendolyn")
            }
        }
        if (message == "§e[NPC] §5Gwendolyn§f: §rGreat! Now hop on into the Minecart and I'll get you on your way!" && inTime()) {
            ChatUtils.clickableChat(
                "Click here to warp to Crystal Hollows!",
                onClick = { HypixelCommands.warp("ch") },
                hover = "§eClick to run /warp ch!",
            )
        }
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (!isEnabled()) return

        if (event.newIsland == IslandType.DWARVEN_MINES && inTime()) {
            ChatUtils.chat("Buy a §2Crystal Hollows Pass §efrom §5Gwendolyn§e!")
        }
        if (event.newIsland == IslandType.CRYSTAL_HOLLOWS) {
            lastWrongPassTime = 0
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.DWARVEN_MINES)
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (!isEnabled()) return

        if (inTime()) {
            event.drawWaypointFilled(location, LorenzColor.YELLOW.toColor())
            event.drawDynamicText(location, "§eBuy Crystal Hollows Pass", 1.3)
        }
    }

    private fun inTime() = lastWrongPassTime + 1000 * 60 * 2 > System.currentTimeMillis()

    fun isEnabled() = HanniMod.feature.misc.crystalHollowsJoin
}
