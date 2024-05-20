package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.TimeLimitedSet
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class ChumBucketHider {

    private val config get() = SkyHanniMod.feature.fishing.chumBucketHider
    private val titleEntity = TimeLimitedSet<Entity>(5.seconds)
    private val hiddenEntities = TimeLimitedSet<Entity>(5.seconds)

    private val patternGroup = RepoPattern.group("chumbucket")
    private val isBucketPattern by patternGroup.pattern(
        "isbucket",
        ".*('s Chum(?:cap)? Bucket)$"
    )
    private val chumAmountPattern by patternGroup.pattern(
        "chumamount",
        ".*/10 §aChums.*"
    )
    private val emptyPattern by patternGroup.pattern(
        "empty",
        "^§[fa]Empty Chum(?:cap)? Bucket$"
    )

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        reset()
    }

    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.enabled.get()) return

        val entity = event.entity
        if (entity !is EntityArmorStand) return

        if (entity in hiddenEntities) {
            event.isCanceled = true
            return
        }

        val name = entity.name

        // First text line

        if (isBucketPattern.matches(name)) {
            if (name.contains(LorenzUtils.getPlayerName()) && !config.hideOwn.get()) return
            titleEntity.add(entity)
            hiddenEntities.add(entity)
            event.isCanceled = true
            return
        }

        // Second text line
        if (chumAmountPattern.matches(name)) {
            val entityLocation = entity.getLorenzVec()
            for (title in titleEntity.toSet()) {
                if (entityLocation.equalsIgnoreY(title.getLorenzVec())) {
                    hiddenEntities.add(entity)
                    event.isCanceled = true
                    return
                }
            }
        }

        // Chum Bucket

        if (config.hideBucket.get() && entity.inventory.any { emptyPattern.matches(it.name) }) {
            val entityLocation = entity.getLorenzVec()
            for (title in titleEntity.toSet()) {
                if (entityLocation.equalsIgnoreY(title.getLorenzVec())) {
                    hiddenEntities.add(entity)
                    event.isCanceled = true
                    return
                }
            }
        }
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.enabled, config.hideBucket, config.hideOwn) { reset() }
    }

    private fun reset() {
        titleEntity.clear()
        hiddenEntities.clear()
    }
}
