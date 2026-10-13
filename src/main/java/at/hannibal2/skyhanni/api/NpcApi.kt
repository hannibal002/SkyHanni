package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.DialogueResponseSentEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketSentEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket
import net.minecraft.resources.Identifier
import java.util.Optional

@SkyHanniModule
object NpcApi {
    val patternGroup = RepoPattern.group("npc-api")

    /**
     * REGEX-TEST: skyblock:dialogue_response
     */
    val dialogueResponseIdPattern by patternGroup.pattern(
        "dialogue.response",
        "skyblock:dialogue_response"
    )

    @HandleEvent(onlyOnSkyblock = true)
    private fun onCustomClickPacketSent(event: PacketSentEvent) {
        val packet = event.packet as? ServerboundCustomClickActionPacket ?: return
        if (!dialogueResponseIdPattern.matches(packet.id().toString())) return

        val (npcId, responseKey) = packet.payload()
            .flatMap { it.asCompound() }
            ?.map {
                it.getString("npcId").orElse(null) to
                    it.getString("responseKey").orElse(null)
            }?.orElse(null) ?: return

        if (npcId == null || responseKey == null) return

        if (DialogueResponseSentEvent(npcId, responseKey).post().isCancelled) {
            event.cancel()
        }
    }

    fun sendNpcResponse(npcId: String, responseKey: String) {
        val optionTag = CompoundTag()
        optionTag.putString("npcId", npcId)
        optionTag.putString("responseKey", responseKey)

        MinecraftCompat.localPlayerOrNull?.connection?.send(
            ServerboundCustomClickActionPacket(Identifier.parse(dialogueResponseIdPattern.toString()), Optional.of(optionTag))
        )
    }
}
