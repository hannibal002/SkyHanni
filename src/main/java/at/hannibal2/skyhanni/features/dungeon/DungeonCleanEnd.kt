package at.hannibal2.hanni.features.dungeon

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.CheckRenderEntityEvent
import at.hannibal2.hanni.events.DamageIndicatorFinalBossEvent
import at.hannibal2.hanni.events.PlaySoundEvent
import at.hannibal2.hanni.events.ReceiveParticleEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.events.entity.EntityHealthUpdateEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.compat.MinecraftCompat
import at.hannibal2.hanni.utils.compat.MinecraftCompat.isLocalPlayer
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityGuardian

@HanniModule
object DungeonCleanEnd {

    private val config get() = HanniMod.feature.dungeon.cleanEnd

    /**
     * REGEX-TEST: §f                §r§cMaster Mode The Catacombs §r§8- §r§eFloor III
     * REGEX-TEST: §f                        §r§cThe Catacombs §r§8- §r§eFloor VI
     * REGEX-TEST: §f                §r§cMaster Mode The Catacombs §r§8- §r§eFloor II
     */
    private val catacombsPattern by RepoPattern.pattern(
        "dungeon.end.chests.spawned",
        "(?:§f)? *§r§c(?:Master Mode )?The Catacombs §r§8- §r§eFloor .*",
    )

    private var bossDone = false
    private var chestsSpawned = false
    private var lastBossId: Int = -1

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onChat(event: HanniChatEvent) {
        if (!config.enabled) return

        val message = event.message

        catacombsPattern.matchMatcher(message) {
            chestsSpawned = true
        }
    }

    private fun shouldBlock(): Boolean {
        if (!config.enabled) return false

        if (!bossDone) return false

        return true
    }

    @HandleEvent
    fun onWorldChange() {
        bossDone = false
        chestsSpawned = false
        lastBossId = -1
    }

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onBossDead(event: DamageIndicatorFinalBossEvent) {
        if (bossDone) return

        if (lastBossId == -1) {
            lastBossId = event.id
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onEntityHealthUpdate(event: EntityHealthUpdateEvent) {
        if (!config.enabled) return
        if (bossDone) return
        if (lastBossId == -1) return
        if (event.entity.entityId != lastBossId) return

        if (event.health <= 0.5) {
            val dungeonFloor = DungeonApi.dungeonFloor
            ChatUtils.chat("§eFloor $dungeonFloor done!", false)
            bossDone = true
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onCheckRender(event: CheckRenderEntityEvent<Entity>) {
        if (!shouldBlock()) return

        val entity = event.entity

        if (entity.isLocalPlayer) return

        if (config.f3IgnoreGuardians &&
            DungeonApi.isOneOf("F3", "M3") &&
            entity is EntityGuardian &&
            entity.entityId != lastBossId &&
            MinecraftCompat.localPlayer.isSneaking
        ) {
            return
        }

        if (chestsSpawned && ((entity is EntityArmorStand && !entity.hasCustomName()) || entity is EntityOtherPlayerMP)) {
            return
        }

        event.cancel()
    }

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (shouldBlock()) {
            event.cancel()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onPlaySound(event: PlaySoundEvent) {
        if (shouldBlock() && !chestsSpawned && event.soundName.startsWith("note.")) {
            event.cancel()
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "dungeon.cleanEndToggle", "dungeon.cleanEnd.enabled")
        event.move(3, "dungeon.cleanEndF3IgnoreGuardians", "dungeon.cleanEnd.F3IgnoreGuardians")
        event.move(75, "dungeon.cleanEnd.F3IgnoreGuardians", "dungeon.cleanEnd.f3IgnoreGuardians")
    }
}
