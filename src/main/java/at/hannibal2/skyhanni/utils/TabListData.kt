package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.utils.LorenzUtils.sorted
import net.minecraft.network.play.server.S38PacketPlayerListItem
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.*

class TabListData {
    private val uuidMap = mutableMapOf<UUID, TabListPlayer>()
    private val tabListMap = mutableMapOf<TabListPlayer, String>()

    class TabListPlayer(var displayName: String, var internalName: String)

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        uuidMap.clear()
        tabListMap.clear()
    }

    @SubscribeEvent
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        val packet = event.packet
        if (packet is S38PacketPlayerListItem) {
            val action = packet.action
            if (action == S38PacketPlayerListItem.Action.UPDATE_LATENCY) return

            val entries = packet.entries
            if (action == S38PacketPlayerListItem.Action.REMOVE_PLAYER) {
//                println("REMOVE_PLAYER")
//                println("old: " + uuidMap.size)
                for (entry in entries) {
                    val profile = entry.profile
                    val id = profile.id
                    val key = uuidMap.remove(id)
                    tabListMap.remove(key)
                }
//                println("new: " + uuidMap.size)
                update()
                return
            }

            val size = entries.size
            if (size != 1) {
                println("wrong size: $size")
                return
            }
            val entry = entries[0]
            val profile = entry.profile
            val id = profile.id
            val name = profile.name
            if (name != null) {
                if (!name.contains("-")) {
                    return
                }
            }

            val text = entry?.displayName?.formattedText ?: ""
            val formattedName = LorenzUtils.stripVanillaMessage(text)
            if (action == S38PacketPlayerListItem.Action.ADD_PLAYER) {
//                println("ADD_PLAYER")
                val tabList = TabListPlayer(formattedName, name)

                if (uuidMap.contains(id)) {
                    val key = uuidMap.remove(id)
                    val internalName = key!!.internalName
                    val displayName = key.displayName
//                    println("")
//                    println("internalName: $internalName")
//                    println("displayName: $displayName")
                    tabListMap.remove(key)
                }

//                println("new name: $name")
//                println("new formattedName: $formattedName")

                uuidMap[id] = tabList
                tabListMap[tabList] = name
                update()
                return
            }

            if (action == S38PacketPlayerListItem.Action.UPDATE_DISPLAY_NAME) {
//                println("UPDATE_DISPLAY_NAME")
                val listPlayer = uuidMap[id]!!
//                println("old: '" + listPlayer.displayName + "'")
//                println("new: '$formattedName'")

                listPlayer.displayName = formattedName
                update()
                return
            }
        }
    }

    private fun update() {
        val result = mutableListOf<String>()
        if (uuidMap.size == 80) {
            var i = 0
            for (tabList in tabListMap.sorted().keys) {
                val contains = uuidMap.values.contains(tabList)
                if (contains) {
                    val displayName = tabList.displayName
                    result.add(displayName)
                    i++
                }
            }
        } else if (uuidMap.isNotEmpty()) return
        val list = result.toList()
        cache = list
        TabListUpdateEvent(list).postAndCatch()

    }

    companion object {
        private var cache = listOf<String>()

        // TODO replace with TabListUpdateEvent
        fun getTabList() = cache
    }
}