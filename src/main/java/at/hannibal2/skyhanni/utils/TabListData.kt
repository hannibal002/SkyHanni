package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.mixins.hooks.tabListGuard
import at.hannibal2.skyhanni.mixins.transformers.AccessorGuiPlayerTabOverlay
import at.hannibal2.skyhanni.utils.LorenzUtils.conditionalTransform
import at.hannibal2.skyhanni.utils.LorenzUtils.transformIf
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import com.google.common.collect.ComparisonChain
import com.google.common.collect.Ordering
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.world.WorldSettings
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

class TabListData {

    companion object {
        private var cache = emptyList<String>()
        private var debugCache: List<String>? = null

        // TODO replace with TabListUpdateEvent
        fun getTabList() = debugCache ?: cache

        fun toggleDebugCommand() {
            if (debugCache != null) {
                LorenzUtils.chat("Disabled tab list debug.")
                debugCache = null
                return
            }
            SkyHanniMod.coroutineScope.launch {
                val clipboard = OSUtils.readFromClipboard() ?: return@launch
                debugCache = clipboard.lines()
                LorenzUtils.chat("Enabled tab list debug with your clipboard.")
            }
        }

        fun copyCommand(args: Array<String>) {
            if (debugCache != null) {
                LorenzUtils.clickableChat("Tab list debug is enabled!", "shdebugtablist")
                return
            }

            val resultList = mutableListOf<String>()
            val noColor = args.size == 1 && args[0] == "true"
            for (line in getTabList()) {
                val tabListLine = line.transformIf({ noColor }) { removeColor() }
                if (tabListLine != "") resultList.add("'$tabListLine'")
            }
            val tabList = Minecraft.getMinecraft().ingameGUI.tabList as AccessorGuiPlayerTabOverlay
            val tabHeader = tabList.header_skyhanni.conditionalTransform(noColor, { unformattedText }, { formattedText })
            val tabFooter = tabList.footer_skyhanni.conditionalTransform(noColor, { unformattedText }, { formattedText })
            val string = "Header:\n\n$tabHeader\n\nBody:\n\n${resultList.joinToString("\n")}\n\nFooter:\n\n$tabFooter"
            OSUtils.copyToClipboard(string)
            LorenzUtils.chat("Tab list copied into the clipboard!")
        }
    }

    private val playerOrdering = Ordering.from(PlayerComparator())

    @SideOnly(Side.CLIENT)
    internal class PlayerComparator : Comparator<NetworkPlayerInfo> {
        override fun compare(o1: NetworkPlayerInfo, o2: NetworkPlayerInfo): Int {
            val team1 = o1.playerTeam
            val team2 = o2.playerTeam
            return ComparisonChain.start().compareTrueFirst(
                o1.gameType != WorldSettings.GameType.SPECTATOR,
                o2.gameType != WorldSettings.GameType.SPECTATOR
            )
                .compare(
                    if (team1 != null) team1.registeredName else "",
                    if (team2 != null) team2.registeredName else ""
                )
                .compare(o1.gameProfile.name, o2.gameProfile.name).result()
        }
    }

    private fun readTabList(): List<String>? {
        val thePlayer = Minecraft.getMinecraft()?.thePlayer ?: return null
        val players = playerOrdering.sortedCopy(thePlayer.sendQueue.playerInfoMap)
        val result = mutableListOf<String>()
        tabListGuard = true
        for (info in players) {
            val name = Minecraft.getMinecraft().ingameGUI.tabList.getPlayerName(info)
            result.add(LorenzUtils.stripVanillaMessage(name))
        }
        tabListGuard = false
        return result.dropLast(1)
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!event.isMod(5)) return

        val tabList = readTabList() ?: return
        if (cache != tabList) {
            cache = tabList
            TabListUpdateEvent(getTabList()).postAndCatch()
        }
    }
}
