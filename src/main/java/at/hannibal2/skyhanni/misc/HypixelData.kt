package at.hannibal2.skyhanni.misc

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.LorenzUtils.removeColorCodes
import net.minecraft.client.Minecraft
import net.minecraft.network.play.server.S38PacketPlayerListItem
import net.minecraft.network.play.server.S3DPacketDisplayScoreboard
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent

class HypixelData {

    companion object {
    var hypixel = false
    var skyblock = false
    var dungeon = false
}

    @SubscribeEvent
    fun onConnect(event: FMLNetworkEvent.ClientConnectedToServerEvent) {
        hypixel = Minecraft.getMinecraft().runCatching {
            !event.isLocal && (thePlayer?.clientBrand?.lowercase()?.contains("hypixel")
                ?: currentServerData?.serverIP?.lowercase()?.contains("hypixel") ?: false)
        }.onFailure { it.printStackTrace() }.getOrDefault(false)
    }

    @SubscribeEvent
    fun onScoreboardChange(event: PacketEvent.ReceiveEvent) {
        if (!hypixel || skyblock || event.packet !is S3DPacketDisplayScoreboard) return
        if (event.packet.func_149371_c() != 1) return
        skyblock = event.packet.func_149370_d() == "SBScoreboard"
    }

    val areaRegex = Regex("§r§b§l(?<area>[\\w]+): §r§7(?<loc>[\\w ]+)§r")

    @SubscribeEvent
    fun onTabUpdate(event: PacketEvent.ReceiveEvent) {
        if (dungeon || !hypixel || event.packet !is S38PacketPlayerListItem ||
            (event.packet.action != S38PacketPlayerListItem.Action.UPDATE_DISPLAY_NAME &&
                    event.packet.action != S38PacketPlayerListItem.Action.ADD_PLAYER)
        ) return
        event.packet.entries.forEach { playerData ->
            val name = playerData?.displayName?.formattedText ?: playerData?.profile?.name ?: return@forEach
            areaRegex.matchEntire(name)?.let { result ->
                dungeon = skyblock && result.groups["area"]?.value == "Dungeon"
                return@forEach
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        skyblock = false
        dungeon = false
    }

    @SubscribeEvent
    fun onDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
        hypixel = false
        skyblock = false
        dungeon = false
    }

    @SubscribeEvent
    fun onStatusBar(event: LorenzChatEvent) {
        if (!hypixel) return

        val message = event.message.removeColorCodes().lowercase()

        if (message.startsWith("your profile was changed to:")) {
            val stripped = message.replace("your profile was changed to:", "").replace("(co-op)", "").trim();
            ProfileJoinEvent(stripped).postAndCatch()
        }
        if (message.startsWith("you are playing on profile:")) {
            val stripped = message.replace("you are playing on profile:", "").replace("(co-op)", "").trim();
            ProfileJoinEvent(stripped).postAndCatch()

        }
    }
}