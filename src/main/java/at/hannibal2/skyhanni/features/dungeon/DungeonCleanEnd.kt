package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.DamageIndicatorFinalBossEvent
import at.hannibal2.skyhanni.events.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchRegex
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityGuardian
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DungeonCleanEnd {

    private var bossDone = false
    private var chestsSpawned = false
    private var lastBossId: Int = -1

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.inDungeons) return
        if (!SkyHanniMod.feature.dungeon.cleanEndToggle) return

        val message = event.message

        if (message.matchRegex("([ ]*)§r§c(The|Master Mode) Catacombs §r§8- §r§eFloor (.*)")) {
            chestsSpawned = true
        }
    }

    private fun shouldBlock(): Boolean {
        if (!LorenzUtils.inDungeons) return false
        if (!SkyHanniMod.feature.dungeon.cleanEndToggle) return false

        if (!bossDone) return false

        return true
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        bossDone = false
        chestsSpawned = false
        lastBossId = -1
    }

    @SubscribeEvent
    fun onBossDead(event: DamageIndicatorFinalBossEvent) {
        if (!LorenzUtils.inDungeons) return
        if (bossDone) return

        if (lastBossId == -1) {
            lastBossId = event.id
        }
    }

    @SubscribeEvent
    fun onEntityHealthUpdate(event: EntityHealthUpdateEvent) {
        if (!LorenzUtils.inDungeons) return
        if (!SkyHanniMod.feature.dungeon.cleanEndToggle) return
        if (bossDone) return
        if (lastBossId == -1) return
        if (event.entity.entityId != lastBossId) return

        if (event.health <= 0) {
            val dungeonFloor = DungeonAPI.dungeonFloor
            LorenzUtils.chat("§eFloor $dungeonFloor done!")
            bossDone = true
        }
    }

    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!shouldBlock()) return

        val entity = event.entity

        if (entity == Minecraft.getMinecraft().thePlayer) return

        if (SkyHanniMod.feature.dungeon.cleanEndF3IgnoreGuardians) {
            if (DungeonAPI.isOneOf("F3", "M3")) {
                if (entity is EntityGuardian) {
                    if (entity.entityId != lastBossId) {
                        if (Minecraft.getMinecraft().thePlayer.isSneaking) {
                            return
                        }
                    }
                }
            }
        }

        if (chestsSpawned) {
            if (entity is EntityArmorStand) {
                if (!entity.hasCustomName()) {
                    return
                }
            }
            if (entity is EntityOtherPlayerMP) {
                return
            }
        }

        event.isCanceled = true
    }

    @SubscribeEvent
    fun onPlayParticle(event: ReceiveParticleEvent) {
        if (shouldBlock()) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onPlaySound(event: PlaySoundEvent) {
        if (shouldBlock() && !chestsSpawned) {
            if (event.soundName.startsWith("note.")) {
                event.isCanceled = true
            }
        }
    }
}