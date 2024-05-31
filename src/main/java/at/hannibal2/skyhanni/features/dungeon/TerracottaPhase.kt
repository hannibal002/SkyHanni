package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.SkyHanniRenderEntityEvent
import at.hannibal2.skyhanni.features.combat.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.entity.EntityLivingBase
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class TerracottaPhase {

    private val config get() = SkyHanniMod.feature.dungeon.terracottaPhase

    private var inTerracottaPhase = false

    private val patternGroup = RepoPattern.group("terracottaphase")
    private val madeItHerePattern by patternGroup.pattern(
        "madeithere",
        "§c\\[BOSS] Sadan§r§f: So you made it all the way here... Now you wish to defy me? Sadan?!"
    )
    private val enoughPattern by patternGroup.pattern(
        "enough",
        "§c\\[BOSS] Sadan§r§f: ENOUGH!"
    )

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return

        if (madeItHerePattern.matches(event.message)) {
            inTerracottaPhase = true
        }

        if (enoughPattern.matches(event.message)) {
            inTerracottaPhase = false
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onRenderLiving(event: SkyHanniRenderEntityEvent.Specials.Pre<EntityLivingBase>) {
        if (isActive() && config.hideDamageSplash && DamageIndicatorManager.isDamageSplash(event.entity)) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onReceiveParticle(event: ReceiveParticleEvent) {
        if (isActive() && config.hideParticles) {
            event.isCanceled = true
        }
    }

    private fun isActive() = isEnabled() && inTerracottaPhase

    private fun isEnabled() =
        DungeonAPI.inDungeon() && DungeonAPI.inBossRoom && DungeonAPI.getCurrentBoss() == DungeonFloor.F6
}
