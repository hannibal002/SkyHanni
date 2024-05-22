package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.ReceiveParticleEvent
import at.hannibal2.skyhanni.events.SkyHanniRenderEntityEvent
import at.hannibal2.skyhanni.features.combat.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.entity.EntityLivingBase
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DungeonPhaseUtils {

    private val config get() = SkyHanniMod.feature.dungeon.terracottaPhase

    private val patternGroup = RepoPattern.group("dungeon.boss.message")
    private val terracottaStartPattern by patternGroup.pattern(
        "f6.terracotta",
        "§c\\[BOSS] Sadan§r§f: So you made it all the way here\\.\\.\\. Now you wish to defy me\\? Sadan\\?!"
    )
    private val giantsStartPattern by patternGroup.pattern(
        "f6.giants",
        "§c\\[BOSS] Sadan§r§f: ENOUGH!"
    )
    private val sadanStartPattern by patternGroup.pattern(
        "f6.sadan",
        "§c\\[BOSS] Sadan§r§f: You did it\\. I understand now, you have earned my respect\\."
    )

    private val stormStartPattern by patternGroup.pattern(
        "f7.storm",
        "§4\\[BOSS] Storm§r§c: §r§cPathetic Maxor, just like expected\\."
    )
    private val goldorStartPattern by patternGroup.pattern(
        "f7.goldor.start",
        "§4\\[BOSS] Goldor§r§c: §r§cWho dares trespass into my domain\\?"
    )
    private val goldorTerminalPattern by patternGroup.pattern(
        "f7.goldor.terminalcomplete",
        "§.(?<playerName>\\w+)§r§a (?:activated|completed) a (?<type>lever|terminal|device)! \\(§r§c(?<currentTerminal>\\d)§r§a/(?<total>\\d)\\)"
    )
    private val goldor5StartPattern by patternGroup.pattern(
        "f7.goldor.5",
        "§aThe Core entrance is opening!"
    )
    private val necronStartPattern by patternGroup.pattern(
        "f7.goldor.start",
        "§4\\[BOSS] Necron§r§c: §r§cYou went further than any human before, congratulations\\."
    )

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        val message = event.message

        if (DungeonAPI.dungeonFloor == "F6" || DungeonAPI.dungeonFloor == "M6") when {
            terracottaStartPattern.matches(message) -> DungeonAPI.dungeonPhase = DungeonPhase.F6_TERRACOTTA
            giantsStartPattern.matches(message) -> DungeonAPI.dungeonPhase = DungeonPhase.F6_GIANTS
            sadanStartPattern.matches(message) -> DungeonAPI.dungeonPhase = DungeonPhase.F6_SADAN
        }

        if (DungeonAPI.dungeonFloor == "F7" || DungeonAPI.dungeonFloor == "M7") {
            goldorTerminalPattern.matchMatcher(message) {
                val currentTerminal = group("currentTerminal").toIntOrNull() ?: return
                val totalTerminals = group("total").toIntOrNull() ?: return
                if (currentTerminal != totalTerminals) return
                when (DungeonAPI.dungeonPhase) {
                    DungeonPhase.F7_GOLDOR_1 -> DungeonAPI.dungeonPhase = DungeonPhase.F7_GOLDOR_2
                    DungeonPhase.F7_GOLDOR_2 -> DungeonAPI.dungeonPhase = DungeonPhase.F7_GOLDOR_3
                    DungeonPhase.F7_GOLDOR_3 -> DungeonAPI.dungeonPhase = DungeonPhase.F7_GOLDOR_4
                    else -> return
                }
            }
            when {
                stormStartPattern.matches(message) -> DungeonAPI.dungeonPhase = DungeonPhase.F7_STORM
                goldorStartPattern.matches(message) -> DungeonAPI.dungeonPhase = DungeonPhase.F7_GOLDOR_1
                goldor5StartPattern.matches(message) -> DungeonAPI.dungeonPhase = DungeonPhase.F7_GOLDOR_5
                necronStartPattern.matches(message) -> DungeonAPI.dungeonPhase = DungeonPhase.F7_NECRON
            }
        }
    }

    //terracotta cleaner
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

    private fun isActive() = isEnabled() && DungeonAPI.dungeonPhase == DungeonPhase.F6_TERRACOTTA

    private fun isEnabled() =
        DungeonAPI.inDungeon() && DungeonAPI.inBossRoom && DungeonAPI.getCurrentBoss() == DungeonFloor.F6
}
