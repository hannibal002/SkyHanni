package at.hannibal2.skyhanni.features.slayer.spider

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.EntityMovementData
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.mob.MobCategory
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.events.entity.EntityMoveEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.MobUtils.mob
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.entity.monster.spider.Spider

@SkyHanniModule
object SlayerSpiderFeatures {
    private val config get() = SlayerApi.config.spider
    private val allTier = mutableSetOf<Mob>()
    private var lastClicked: Mob? = null
    val stuckMobs = mutableSetOf<Mob>()

    /**
     * REGEX-TEST: You need to kill the Broodfather's hatchlings before it can be damaged again!
     */
    private val damageBroodlingPattern by RepoPattern.pattern(
        "slayer.spider.damagebroodling",
        "You need to kill the Broodfather's hatchlings before it can be damaged again!"
    )

    @HandleEvent(onlyOnSkyblock = true)
    private fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        val mob = event.mob
        if (mob.isRightTier()) {
            allTier.add(mob)
        }
    }

    private fun Mob.isRightTier() = category == MobCategory.SLAYER && (levelOrTier in 3..5) && name == "Tarantula Broodfather"

    @HandleEvent(onlyOnSkyblock = true)
    private fun onEntityClick(event: EntityClickEvent) {
        if (event.action != EntityClickEvent.ActionType.ATTACK) return
        val mob = event.clickedEntity.mob ?: return
        if (mob in allTier) {
            lastClicked = mob
        }
    }

    @HandleEvent
    private fun onChat(event: SystemMessageEvent.Allow) {
        if (!damageBroodlingPattern.matches(event.cleanMessage)) return

        val mob = lastClicked ?: return
        mob.highlight(config.highlightInvincibleColor, condition = { config.highlightInvincible && mob in stuckMobs })
        stuckMobs.add(mob)
        EntityMovementData.addToTrack(mob)
    }

    @HandleEvent
    private fun onPlayerMove(event: EntityMoveEvent<Spider>) {
        val mob = event.entity.mob ?: return
        if (mob in stuckMobs) {
            stuckMobs.remove(mob)
            mob.removeHighlight()
        }
    }

    @HandleEvent(WorldChangeEvent::class)
    private fun onWorldChange() {
        allTier.clear()
        lastClicked = null
        stuckMobs.clear()
    }
}
