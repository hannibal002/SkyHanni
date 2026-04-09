package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.events.entity.EntityMaxHealthUpdateEvent
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.AllEntitiesGetter
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.hasMaxHealth
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.golem.IronGolem
import net.minecraft.world.entity.monster.Endermite
import net.minecraft.world.entity.monster.MagmaCube
import net.minecraft.world.entity.monster.Slime

@SkyHanniModule
object HighlightMiningCommissionMobs {

    private val config get() = SkyHanniMod.feature.mining

    // TODO Commission API
    private var active = listOf<(LivingEntity) -> Boolean>()

    // TODO Commission API
    private val commissionMobs: Map<String, (LivingEntity) -> Boolean> = mapOf(
        // Dwarven Mines
        "Goblin Slayer" to { it.name.string == "Goblin " || it.name.string == "Weakling " }, // Dwarven Mines + Crystal Hollows
        "Star Sentry Puncher" to { it.name.string == "Crystal Sentry" },
        "Glacite Walker Slayer" to { it.name.string == "Ice Walker" },
        "Golden Goblin Slayer" to { it.name.string.contains("Golden Goblin") },
        "Treasure Hoarder Puncher" to {
            // typo is intentional, that's on hypixel's end
            @Suppress("SpellCheckingInspection")
            it.name.string == "Treasuer Hunter"
        },

        // Crystal Hollows
        "Automaton Slayer" to { it is IronGolem && (it.hasMaxHealth(15_000) || it.hasMaxHealth(20_000)) },
        "Team Treasurite Member Slayer" to { it.name.string == "Team Treasurite" },
        "Yog Slayer" to { it is MagmaCube && it.hasMaxHealth(35_000) },
        "Thyst Slayer" to { it is Endermite && it.hasMaxHealth(5_000) },
        "Corleone Slayer" to { it.hasMaxHealth(1_000_000) && it.name.string == "Team Treasurite" },
        "Sludge Slayer" to { it is Slime && (it.hasMaxHealth(5_000) || it.hasMaxHealth(10_000) || it.hasMaxHealth(25_000)) },

        // new commissions
    )

    @OptIn(AllEntitiesGetter::class)
    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        if (!event.repeatSeconds(2)) return

        // TODO: optimize to just update when the commissions change
        val entities = EntityUtils.getEntities<LivingEntity>()
        for (isMob in active) {
            for (entity in entities) {
                if (isMob(entity)) {
                    RenderLivingEntityHelper.setEntityColor(
                        entity,
                        LorenzColor.YELLOW.toColor().addAlpha(127),
                    ) { isEnabled() && isMob in active }
                }
            }
        }
    }

    @HandleEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        if (!isEnabled()) return

        // TODO Commission API
        active = commissionMobs.filter { (name, _) ->
            event.tabList.findLast { line -> line.string.removeColor().trim().startsWith(name) }
                ?.let { !it.string.endsWith("DONE") } ?: false
        }.values.toList()
    }

    @HandleEvent
    fun onEntityHealthUpdate(event: EntityMaxHealthUpdateEvent) {
        if (!isEnabled()) return

        val entity = event.entity
        for (isMob in active) {
            if (isMob(entity)) {
                RenderLivingEntityHelper.setEntityColor(
                    entity,
                    LorenzColor.YELLOW.toColor().addAlpha(127),
                ) { isEnabled() && isMob in active }
            }
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.mining", "mining")
    }

    fun isEnabled() = config.highlightCommissionMobs && (IslandType.DWARVEN_MINES.isInIsland() || IslandType.CRYSTAL_HOLLOWS.isInIsland())
}
