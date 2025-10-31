package at.hannibal2.hanni.features.dungeon

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.ReceiveParticleEvent
import at.hannibal2.hanni.events.HanniRenderEntityEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.features.combat.damageindicator.DamageIndicatorManager
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import net.minecraft.entity.item.EntityArmorStand

@HanniModule
object TerracottaPhase {

    private val config get() = HanniMod.feature.dungeon.terracottaPhase

    private var inTerracottaPhase = false

    private val patternGroup = RepoPattern.group("dungeon.terracotta")
    private val terracottaStartPattern by patternGroup.pattern(
        "start",
        "§c\\[BOSS] Sadan§r§f: So you made it all the way here... Now you wish to defy me\\? Sadan\\?!",
    )
    private val terracottaEndPattern by patternGroup.pattern(
        "end",
        "§c\\[BOSS] Sadan§r§f: ENOUGH!",
    )

    @HandleEvent
    fun onChat(event: HanniChatEvent) {
        if (terracottaStartPattern.matches(event.message)) {
            inTerracottaPhase = true
        } else if (terracottaEndPattern.matches(event.message)) {
            inTerracottaPhase = false
        }
    }

    @HandleEvent(priority = HandleEvent.HIGH)
    fun onRenderLiving(event: HanniRenderEntityEvent.Specials.Pre<EntityArmorStand>) {
        if (isActive() && config.hideDamageSplash && DamageIndicatorManager.isDamageSplash(event.entity)) {
            event.cancel()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (isActive() && config.hideParticles) {
            event.cancel()
        }
    }

    private fun isActive() = inTerracottaPhase && isEnabled()

    private fun isEnabled() = DungeonApi.inBossRoom && DungeonApi.getCurrentBoss() == DungeonFloor.F6
}
