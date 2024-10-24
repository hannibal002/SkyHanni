package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.DamageIndicatorFinalBossEvent
import at.hannibal2.skyhanni.events.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityGuardian

@SkyHanniModule
object DungeonCleanEnd {

    private val config get() = SkyHanniMod.feature.dungeon.cleanEnd

    /**
     * REGEX-TEST: §f                §r§cMaster Mode The Catacombs §r§8- §r§eFloor III
     * REGEX-TEST: §f                        §r§cThe Catacombs §r§8- §r§eFloor VI
     * REGEX-TEST: §f                §r§cMaster Mode Catacombs §r§8- §r§eFloor II
     */
    private val catacombsPattern by RepoPattern.pattern(
        "dungeon.end.chests.spawned",
        "(?:§f)?( *)§r§c(Master Mode )?The Catacombs §r§8- §r§eFloor (.*)",
    )

    private var bossDone = false
    private var chestsSpawned = false
    private var lastBossId: Int = -1

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        if (!DungeonAPI.inDungeon()) return
        if (!config.enabled) return

        val message = event.message

        catacombsPattern.matchMatcher(message) {
            chestsSpawned = true
        }
    }

    private fun shouldBlock(): Boolean {
        if (!DungeonAPI.inDungeon()) return false
        if (!config.enabled) return false

        if (!bossDone) return false

        return true
    }

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        bossDone = false
        chestsSpawned = false
        lastBossId = -1
    }

    @HandleEvent
    fun onBossDead(event: DamageIndicatorFinalBossEvent) {
        if (!DungeonAPI.inDungeon()) return
        if (bossDone) return

        if (lastBossId == -1) {
            lastBossId = event.id
        }
    }

    @HandleEvent
    fun onEntityHealthUpdate(event: EntityHealthUpdateEvent) {
        if (!DungeonAPI.inDungeon()) return
        if (!config.enabled) return
        if (bossDone) return
        if (lastBossId == -1) return
        if (event.entity.entityId != lastBossId) return

        if (event.health <= 0.5) {
            val dungeonFloor = DungeonAPI.dungeonFloor
            ChatUtils.chat("§eFloor $dungeonFloor done!", false)
            bossDone = true
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onCheckRender(event: CheckRenderEntityEvent<Entity>) {
        if (!shouldBlock()) return

        val entity = event.entity

        if (entity == Minecraft.getMinecraft().thePlayer) return

        if (config.F3IgnoreGuardians &&
            DungeonAPI.isOneOf("F3", "M3") &&
            entity is EntityGuardian &&
            entity.entityId != lastBossId &&
            Minecraft.getMinecraft().thePlayer.isSneaking
        ) {
            return
        }

        if (chestsSpawned && ((entity is EntityArmorStand && !entity.hasCustomName()) || entity is EntityOtherPlayerMP)) {
            return
        }

        event.cancel()
    }

    @HandleEvent
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (shouldBlock()) {
            event.cancel()
        }
    }

    @HandleEvent
    fun onPlaySound(event: PlaySoundEvent) {
        if (shouldBlock() && !chestsSpawned && event.soundName.startsWith("note.")) {
            event.cancel()
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "dungeon.cleanEndToggle", "dungeon.cleanEnd.enabled")
        event.move(3, "dungeon.cleanEndF3IgnoreGuardians", "dungeon.cleanEnd.F3IgnoreGuardians")
    }
}
