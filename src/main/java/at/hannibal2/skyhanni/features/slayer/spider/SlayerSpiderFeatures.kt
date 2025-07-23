package at.hannibal2.skyhanni.features.slayer.spider

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.events.entity.EntityMoveEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.MobUtils.mob
import net.minecraft.entity.monster.EntitySpider
import net.minecraft.network.play.client.C02PacketUseEntity

@SkyHanniModule
object SlayerSpiderFeatures {
    private val config get() = SlayerApi.config.spider
    private val allTier5 = mutableSetOf<Mob>()
    private var lastClickedTier5: Mob? = null
    val stuckTier5 = mutableSetOf<Mob>()

    @HandleEvent(onlyOnSkyblock = true)
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        val mob = event.mob
        if (mob.isTier5()) {
            allTier5.add(mob)
        }
    }

    private fun Mob.isTier5() = mobType == Mob.Type.SLAYER && levelOrTier == 5 && name == "Tarantula Broodfather"

    @HandleEvent(onlyOnSkyblock = true)
    fun onClickEntity(event: EntityClickEvent) {
        if (event.action != C02PacketUseEntity.Action.ATTACK) return
        val mob = event.clickedEntity.mob ?: return
        if (mob in allTier5) {
            lastClickedTier5 = mob
        }
    }

    @HandleEvent
    fun onChat(event: SystemMessageEvent) {
        if (event.message != "§cYou need to kill the Broodfather's hatchlings before it can be damaged again!") return

        val mob = lastClickedTier5 ?: return
        mob.highlight(config.highlightInvincibleColor, condition = { config.highlightInvincible && mob in stuckTier5 })
        stuckTier5.add(mob)

    }

    @HandleEvent
    fun onPlayerMove(event: EntityMoveEvent<EntitySpider>) {
        val mob = event.entity.mob ?: return
        if (mob in stuckTier5) {
            stuckTier5.remove(mob)
            mob.removeHighlight()
        }
    }

    @HandleEvent(WorldChangeEvent::class)
    fun onWorldChange() {
        allTier5.clear()
        lastClickedTier5 = null
        stuckTier5.clear()
    }
}
