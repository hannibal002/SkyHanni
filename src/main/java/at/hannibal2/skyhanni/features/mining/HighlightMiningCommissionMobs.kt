package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.EntityMaxHealthUpdateEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.monster.EntityIronGolem
import net.minecraft.entity.monster.EntityMagmaCube
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class HighlightMiningCommissionMobs {

    private var active = listOf<MobType>()

    enum class MobType(val commissionName: String, val isMob: (EntityLivingBase) -> Boolean) {

        // Dwarven Mines
        STAR_PUNCHER("Star Sentry Puncher", { it.name == "Crystal Sentry" }), // TODO this is still untested
        GOBLIN_SLAYER("Goblin Slayer", { it.name == "Goblin " }), // TODO this is still untested
        ICE_WALKER("Ice Walker Slayer", { it.name == "Ice Walker" }), // TODO this is still untested

        // Crystal Hollows
        AUTOMATON("Automaton Slayer", { it is EntityIronGolem }),
        TEAM_TREASURITE_MEMBER("Team Treasurite Member Slayer", { it.name == "Team Treasurite" }),
        YOG("Yog Slayer", { it is EntityMagmaCube }), // TODO this is still untested

        ;
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!event.isMod(40)) return

        val entities = Minecraft.getMinecraft().theWorld.loadedEntityList.filterIsInstance<EntityLivingBase>()
        for ((type, entity) in active.flatMap { type -> entities.map { type to it } }) {
            if (type.isMob(entity)) {
                RenderLivingEntityHelper.setEntityColor(entity, LorenzColor.YELLOW.toColor().withAlpha(127))
                { isEnabled() && type in active }
            }
        }
    }

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        if (!isEnabled()) return

        MobType.values().filter { type ->
            event.tabList.find { line -> line.contains(type.commissionName) }?.let { !it.endsWith("§aDONE") } ?: false
        }.let {
            if (it != active) {
                active = it
            }
        }
    }

    @SubscribeEvent
    fun onEntityHealthUpdate(event: EntityMaxHealthUpdateEvent) {
        if (!isEnabled()) return

        val entity = event.entity
        for (type in active) {
            if (type.isMob(entity)) {
                RenderLivingEntityHelper.setEntityColor(entity, LorenzColor.YELLOW.toColor().withAlpha(127))
                { isEnabled() && type in active }
            }
        }
    }

    fun isEnabled() = LorenzUtils.inIsland(IslandType.DWARVEN_MINES) || LorenzUtils.inIsland(IslandType.CRYSTAL_HOLLOWS)
}
