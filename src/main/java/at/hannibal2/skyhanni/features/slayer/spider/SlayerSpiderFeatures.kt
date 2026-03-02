package at.hannibal2.skyhanni.features.slayer.spider

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.EntityMovementData
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.events.entity.EntityMoveEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.MobUtils.mob
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.world.entity.monster.spider.Spider

@SkyHanniModule
object SlayerSpiderFeatures {
    private val config get() = SlayerApi.config.spider
    private val allTier = mutableSetOf<Mob>()
    private var lastClicked: Mob? = null
    val stuckMobs = mutableSetOf<Mob>()

    @HandleEvent(onlyOnSkyblock = true)
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        val mob = event.mob
        if (mob.isRightTier()) {
            allTier.add(mob)
        }
    }

    private fun Mob.isRightTier() = mobType == Mob.Type.SLAYER && (levelOrTier in 3..5) && name == "Tarantula Broodfather"

    @HandleEvent(onlyOnSkyblock = true)
    fun onClickEntity(event: EntityClickEvent) {
        if (event.action != ServerboundInteractPacket.ActionType.ATTACK) return
        val mob = event.clickedEntity.mob ?: return
        if (mob in allTier) {
            lastClicked = mob
        }
    }

    @HandleEvent
    fun onChat(event: SystemMessageEvent.Allow) {
        if (event.message != "§cYou need to kill the Broodfather's hatchlings before it can be damaged again!") return

        val mob = lastClicked ?: return
        mob.highlight(config.highlightInvincibleColor, condition = { config.highlightInvincible && mob in stuckMobs })
        stuckMobs.add(mob)
        EntityMovementData.addToTrack(mob)
    }

    @HandleEvent
    fun onPlayerMove(event: EntityMoveEvent<Spider>) {
        val mob = event.entity.mob ?: return
        if (mob in stuckMobs) {
            stuckMobs.remove(mob)
            mob.removeHighlight()
        }
    }

    @HandleEvent(WorldChangeEvent::class)
    fun onWorldChange() {
        allTier.clear()
        lastClicked = null
        stuckMobs.clear()
    }
}
