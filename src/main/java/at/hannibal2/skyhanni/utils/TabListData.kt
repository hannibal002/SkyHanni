package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.mixins.hooks.tabListGuard
import com.google.common.collect.ComparisonChain
import com.google.common.collect.Ordering
import net.minecraft.client.Minecraft
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.world.WorldSettings
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.util.*

class TabListData {

    companion object {
        private var cache = listOf<String>()

        // TODO replace with TabListUpdateEvent
        fun getTabList() = cache
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
        val result: MutableList<String> = ArrayList()
        tabListGuard = true
        for (info in players) {
            val name = Minecraft.getMinecraft().ingameGUI.tabList.getPlayerName(info)
            result.add(LorenzUtils.stripVanillaMessage(name))
        }
        tabListGuard = false
        return result.dropLast(1)
    }

    private var ticks = 0

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return

        if (ticks++ % 5 != 0) return

        val tabList = readTabList() ?: return
        if (cache != tabList) {
            cache = tabList
            TabListUpdateEvent(cache).postAndCatch()
        }
    }

    //    private val uuidMap = mutableMapOf<UUID, TabListPlayer>()
//    private val tabListMap = mutableMapOf<TabListPlayer, String>()

//    class TabListPlayer(var displayName: String, var internalName: String)

//    @SubscribeEvent
//    fun onWorldChange(event: WorldEvent.Load) {
//        uuidMap.clear()
//        tabListMap.clear()
//    }

//    @SubscribeEvent
//    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
//        val packet = event.packet
//        if (packet is S38PacketPlayerListItem) {
//            val action = packet.action
//            if (action == S38PacketPlayerListItem.Action.UPDATE_LATENCY) return
//
//            val entries = packet.entries
//            if (action == S38PacketPlayerListItem.Action.REMOVE_PLAYER) {
////                println("REMOVE_PLAYER")
////                println("old: " + uuidMap.size)
//                for (entry in entries) {
//                    val profile = entry.profile
//                    val id = profile.id
//                    val key = uuidMap.remove(id)
//                    tabListMap.remove(key)
//                }
////                println("new: " + uuidMap.size)
//                update()
//                return
//            }
//
//            val size = entries.size
//            if (size != 1) {
//                println("wrong size: $size")
//                return
//            }
//            val entry = entries[0]
//            val profile = entry.profile
//            val id = profile.id
//            val name = profile.name
//            if (name != null) {
//                if (!name.contains("-")) {
//                    return
//                }
//            }
//
//            val text = entry?.displayName?.formattedText ?: ""
//            val formattedName = LorenzUtils.stripVanillaMessage(text)
//            if (action == S38PacketPlayerListItem.Action.ADD_PLAYER) {
////                println("ADD_PLAYER")
//                val tabList = TabListPlayer(formattedName, name)
//
//                if (uuidMap.contains(id)) {
//                    val key = uuidMap.remove(id)
//                    val internalName = key!!.internalName
//                    val displayName = key.displayName
////                    println("")
////                    println("internalName: $internalName")
////                    println("displayName: $displayName")
//                    tabListMap.remove(key)
//                }
//
////                println("new name: $name")
////                println("new formattedName: $formattedName")
//
//                uuidMap[id] = tabList
//                tabListMap[tabList] = name
//                update()
//                return
//            }
//
//            if (action == S38PacketPlayerListItem.Action.UPDATE_DISPLAY_NAME) {
////                println("UPDATE_DISPLAY_NAME")
//                val listPlayer = uuidMap[id]
//                listPlayer?.let {
//                    it.displayName = formattedName
//                    update()
//                }
////                println("old: '" + listPlayer.displayName + "'")
////                println("new: '$formattedName'")
//
//                return
//            }
//        }
//    }

//    private fun update() {
//        val result = mutableListOf<String>()
//        if (uuidMap.size == 80) {
//            var i = 0
//            for (tabList in tabListMap.sorted().keys) {
//                val contains = uuidMap.values.contains(tabList)
//                if (contains) {
//                    val displayName = tabList.displayName
//                    result.add(displayName)
//                    i++
//                }
//            }
//        } else if (uuidMap.isNotEmpty()) return
//        val list = result.toList()
//        cache = list
//        TabListUpdateEvent(list).postAndCatch()
//    }
}