package at.hannibal2.skyhanni.features.rift.area.colosseum

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object BactePhase {

    /**
     * REGEX-TEST: §2﴾ §8[§7Lv10§8] §l§aBa§r §a800§f/§a1,000§c❤ §2﴿
     */
    private val namePattern by RepoPattern.pattern(
        "rift.colosseum.bacte.name",
        "§2﴾ §8\\[§7Lv\\d+§8\\] §l§a(?<name>.*)§r §.[\\d.,]+§f\\/§a[\\d.,]+§c❤ §2﴿",
    )

    enum class BactePhase(val displayName: String) {
        NOT_ACTIVE("Not Active"),
        PHASE_1("Phase 1"),
        PHASE_2("Phase 2"),
        PHASE_3("Phase 3"),
        PHASE_4("Phase 4"),
        PHASE_5("Phase 5"),
        ;
    }

    var currentPhase = BactePhase.NOT_ACTIVE
    private var bacte: Mob? = null

    @SubscribeEvent
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        if (event.mob.name != "Bacte") return
        bacte = event.mob
    }

    @SubscribeEvent
    fun onMobDespawn(event: MobEvent.DeSpawn.SkyblockMob) {
        if (event.mob == bacte) {
            currentPhase = BactePhase.NOT_ACTIVE
            bacte = null
        }
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        val bacte = bacte ?: return

        val name = bacte.armorStand?.name ?: return

        namePattern.matchMatcher(name) {
            currentPhase = when (group("name").length) {
                1 -> BactePhase.PHASE_1
                2 -> BactePhase.PHASE_2
                3 -> BactePhase.PHASE_3
                4 -> BactePhase.PHASE_4
                5 -> BactePhase.PHASE_5
                else -> BactePhase.NOT_ACTIVE
            }
            return
        }

        currentPhase = BactePhase.NOT_ACTIVE
    }

    @SubscribeEvent
    fun onWorldRender(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        if (currentPhase == BactePhase.NOT_ACTIVE) return
        val bacte = bacte ?: return
        event.drawDynamicText(
            bacte.baseEntity.getLorenzVec().add(-0.5, 0.0, -0.5),
            "Phase: ${currentPhase.displayName}",
            1.0,
        )
    }

    fun isEnabled() = RiftAPI.inRift() && RiftAPI.inColosseum() && SkyHanniMod.feature.rift.area.colosseum.showBactePhase

}
