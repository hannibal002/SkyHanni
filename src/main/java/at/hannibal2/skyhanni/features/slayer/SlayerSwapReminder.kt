package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.mob.Mob.Companion.belongsToPlayer
import at.hannibal2.skyhanni.data.mob.MobCategory
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.BossHealthChangeEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object SlayerSwapReminder {

    private val config get() = SlayerApi.config.swapReminder
    private val formattedTitle get() = config.titleText.replace("&", "§")

    private var hasRemindedForCurrentBoss = false

    private fun showReminder() {
        TitleManager.sendTitle(
            titleText = formattedTitle,
            duration = 2.seconds,
        )

        if (config.playSound) {
            SoundUtils.playPlingSound()
        }
    }

    private fun stopReminder() {
        TitleManager.conditionallyStopTitle { activeTitle ->
            activeTitle == formattedTitle
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onBossHealthChange(event: BossHealthChangeEvent) {
        if (!isActive()) return
        if (hasRemindedForCurrentBoss) return

        val mob = event.entityData.mob
        if (mob.category != MobCategory.SLAYER || !mob.belongsToPlayer()) return

        val lastHealth = event.lastHealth
        val maxHealth = event.maxHealth

        // Ignore uninitialized or dead mob health states
        if (maxHealth <= 0 || lastHealth <= 0) return

        val hpPercentage = (lastHealth * 100.0) / maxHealth
        if (hpPercentage >= config.hpThreshold) return

        hasRemindedForCurrentBoss = true
        showReminder()
    }

    @HandleEvent
    private fun onMobDeSpawn(event: MobEvent.DeSpawn.SkyblockMob) {
        if (event.mob.category != MobCategory.SLAYER || !event.mob.belongsToPlayer()) return

        hasRemindedForCurrentBoss = false
        stopReminder()
    }

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class, onlyOnSkyblock = true)
    private fun onGuiRenderOverlay() {
        if (!isActive() || !hasRemindedForCurrentBoss) return

        val display = listOf(
            Renderable.text(formattedTitle)
        )

        config.position.renderRenderables(
            renderables = display,
            posLabel = "Slayer Swap Reminder",
        )
    }

    private fun isActive() = config.enabled && SlayerApi.isInBossFight() && !IslandType.THE_RIFT.isInIsland()
}
