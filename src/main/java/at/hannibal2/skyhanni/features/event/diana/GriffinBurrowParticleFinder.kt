package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.BurrowDetectEvent
import at.hannibal2.skyhanni.events.BurrowDugEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GriffinBurrowParticleFinder {
    private val config get() = SkyHanniMod.feature.event.diana

    private val recentlyDugParticleBurrows = mutableListOf<LorenzVec>()
    private val burrows = mutableMapOf<LorenzVec, Burrow>()
    var lastDugParticleBurrow: LorenzVec? = null

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.burrowsSoopyGuess) return
        if (LorenzUtils.skyBlockIsland != IslandType.HUB) return
        val packet = event.packet

        if (packet is S2APacketParticles) {

            val particleType = ParticleType.getParticleType(packet)
            if (particleType != null) {

                val location = packet.toLorenzVec().toBlocPos().down().toLorenzVec()
                if (recentlyDugParticleBurrows.contains(location)) return
                val burrow = burrows.getOrPut(location) { Burrow(location) }

                when (particleType) {
                    ParticleType.FOOTSTEP -> burrow.hasFootstep = true
                    ParticleType.ENCHANT -> burrow.hasEnchant = true
                    ParticleType.EMPTY -> burrow.type = 0
                    ParticleType.MOB -> burrow.type = 1
                    ParticleType.TREASURE -> burrow.type = 2
                }

                if (burrow.hasEnchant && burrow.hasFootstep && burrow.type != -1) {
                    if (!burrow.found) {
                        BurrowDetectEvent(burrow.location, burrow.getType()).postAndCatch()
                    }
                    burrow.found = true
                }
            }
        }
    }

    private enum class ParticleType(val check: S2APacketParticles.() -> Boolean) {
        EMPTY({
            particleType == net.minecraft.util.EnumParticleTypes.CRIT_MAGIC && particleCount == 4 && particleSpeed == 0.01f && xOffset == 0.5f && yOffset == 0.1f && zOffset == 0.5f
        }),
        MOB({
            particleType == net.minecraft.util.EnumParticleTypes.CRIT && particleCount == 3 && particleSpeed == 0.01f && xOffset == 0.5f && yOffset == 0.1f && zOffset == 0.5f

        }),
        TREASURE({
            particleType == net.minecraft.util.EnumParticleTypes.DRIP_LAVA && particleCount == 2 && particleSpeed == 0.01f && xOffset == 0.35f && yOffset == 0.1f && zOffset == 0.35f
        }),
        FOOTSTEP({
            particleType == net.minecraft.util.EnumParticleTypes.FOOTSTEP && particleCount == 1 && particleSpeed == 0.0f && xOffset == 0.05f && yOffset == 0.0f && zOffset == 0.05f
        }),
        ENCHANT({
            particleType == net.minecraft.util.EnumParticleTypes.ENCHANTMENT_TABLE && particleCount == 5 && particleSpeed == 0.05f && xOffset == 0.5f && yOffset == 0.4f && zOffset == 0.5f
        });

        companion object {

            fun getParticleType(packet: S2APacketParticles): ParticleType? {
                if (!packet.isLongDistance) return null
                for (type in entries) {
                    if (type.check(packet)) {
                        return type
                    }
                }
                return null
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        burrows.clear()
        recentlyDugParticleBurrows.clear()
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!config.burrowsSoopyGuess) return
        if (LorenzUtils.skyBlockIsland != IslandType.HUB) return
        val message = event.message
        if (message.startsWith("§eYou dug out a Griffin Burrow!") ||
            message == "§eYou finished the Griffin burrow chain! §r§7(4/4)"
        ) {
            val burrow = lastDugParticleBurrow
            if (burrow != null) {
                recentlyDugParticleBurrows.add(burrow)
                lastDugParticleBurrow = null
                burrows.remove(burrow)?.let {
                    if (it.found) {
                        BurrowDugEvent(it.location).postAndCatch()
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onBlockClick(event: BlockClickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.burrowsSoopyGuess) return
        if (LorenzUtils.skyBlockIsland != IslandType.HUB) return

        val pos = event.position
        if (event.itemInHand?.isSpade != true || pos.getBlockAt() !== Blocks.grass) return

        if (burrows.containsKey(pos)) {
            lastDugParticleBurrow = pos
        }
    }

    private val ItemStack.isSpade
        get() = getInternalName().equals("ANCESTRAL_SPADE")

    class Burrow(
        var location: LorenzVec,
        var hasFootstep: Boolean = false,
        var hasEnchant: Boolean = false,
        var type: Int = -1,
        var found: Boolean = false,
    ) {

        fun getType(): BurrowType {
            return when (this.type) {
                0 -> BurrowType.START
                1 -> BurrowType.MOB
                2 -> BurrowType.TREASURE
                else -> BurrowType.UNKNOWN
            }
        }
    }
}
