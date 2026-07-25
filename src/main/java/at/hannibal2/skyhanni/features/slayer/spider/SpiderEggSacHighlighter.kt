package at.hannibal2.skyhanni.features.slayer.spider

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.entity.EntityRemovedEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.features.slayer.SlayerType
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.EntityUtils.getEntitiesNearby
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.compat.EntityCompat.deceased
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.entity.decoration.ArmorStand

@SkyHanniModule
object SpiderEggSacHighlighter {

    private val config get() = SlayerApi.config.spider
    private val highlightedEggSacs = mutableSetOf<ArmorStand>()

    /**
     * REGEX-TEST: 5s 1/3
     * REGEX-TEST: 12s 3/4
     */
    private val eggSacTimerPattern by RepoPattern.pattern(
        "slayer.spider.eggsac.timer",
        "\\d+s \\d+/\\d+",
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick(event: SkyHanniTickEvent) {
        if (!event.isMod(5)) return
        if (!isEnabled()) {
            if (highlightedEggSacs.isNotEmpty()) clearEggSacs()
            return
        }

        val armorStands = getEntitiesNearby<ArmorStand>(20.0)
        val shootMeStands = armorStands.filter {
            it.cleanName.equals("SHOOT ME!", ignoreCase = true)
        }
        val timerStands = armorStands.filter {
            eggSacTimerPattern.matches(it.cleanName)
        }

        val currentEggSacs = timerStands
            .filter { timer ->
                shootMeStands.any { it.getLorenzVec().distance(timer.getLorenzVec()) < 2.5 }
            }
            .flatMap { it.getEggSacParts(armorStands) }
            .toSet()

        highlightedEggSacs.removeAll {
            val shouldRemove = it.deceased || it !in currentEggSacs
            if (shouldRemove) RenderLivingEntityHelper.removeEntityColor(it)
            shouldRemove
        }

        currentEggSacs.forEach(::highlightEggSac)
    }

    private fun ArmorStand.getEggSacParts(armorStands: List<ArmorStand>): List<ArmorStand> {
        val location = getLorenzVec()
        return listOf(this) + armorStands.filter {
            it.cleanName == "Armor Stand" && it.getLorenzVec().distance(location) < 1.2
        }
    }

    private fun highlightEggSac(eggSac: ArmorStand) {
        if (!highlightedEggSacs.add(eggSac)) return

        RenderLivingEntityHelper.setEntityColor(
            eggSac,
            config.eggSacHighlightColor.toColor(),
        ) { isEnabled() && eggSac in highlightedEggSacs }
    }

    @HandleEvent
    fun onEntityRemoved(event: EntityRemovedEvent<ArmorStand>) {
        if (highlightedEggSacs.remove(event.entity)) {
            RenderLivingEntityHelper.removeEntityColor(event.entity)
        }
    }

    @HandleEvent(WorldChangeEvent::class)
    fun onWorldChange() = clearEggSacs()

    private fun clearEggSacs() {
        if (highlightedEggSacs.isEmpty()) return
        highlightedEggSacs.forEach { RenderLivingEntityHelper.removeEntityColor(it) }
        highlightedEggSacs.clear()
    }

    private fun isEnabled() = config.highlightEggSacs &&
        SlayerApi.activeType == SlayerType.TARANTULA &&
        SlayerApi.isInBossFight()
}
