package at.hannibal2.skyhanni.features.misc.effects

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.effect.EffectApi
import at.hannibal2.skyhanni.data.effect.NonGodPotEffect
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.effects.EffectDurationChangeEvent
import at.hannibal2.skyhanni.events.effects.EffectDurationChangeType
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraApi
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playPlingSound
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.TimeUtils.timerColor
import at.hannibal2.skyhanni.utils.Timer
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sorted
import at.hannibal2.skyhanni.utils.collection.TimeLimitedSet
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object NonGodPotEffectDisplay {

    private val config get() = SkyHanniMod.feature.misc.nonGodPotEffect
    private val effectDuration = mutableMapOf<NonGodPotEffect, Timer>()
    private val setRecently: TimeLimitedSet<NonGodPotEffect> = TimeLimitedSet(5.seconds)
    private var display = emptyList<String>()

    fun isActive(effect: NonGodPotEffect): Boolean = effectDuration.any { it.key == effect && !it.value.ended }

    @HandleEvent
    private fun onProfileJoin() {
        effectDuration.clear()
        display = emptyList()
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        if (event.message == "§aYou cleared all of your active effects!") {
            effectDuration.clear()
            update()
        }
    }

    @HandleEvent
    private fun onEffectUpdate(event: EffectDurationChangeEvent) {
        val duration = event.duration ?: Duration.ZERO
        when (event.durationChangeType) {
            EffectDurationChangeType.ADD -> {
                if (setRecently.contains(event.effect)) return
                val existing = effectDuration[event.effect]?.duration ?: Duration.ZERO
                effectDuration[event.effect] = Timer(existing + duration)
            }

            EffectDurationChangeType.SET -> {
                effectDuration[event.effect] = Timer(duration)
                setRecently.add(event.effect)
            }

            EffectDurationChangeType.REMOVE -> {
                effectDuration.remove(event.effect)
            }

            EffectDurationChangeType.PARTIAL_SET -> {
                val existing = effectDuration[event.effect]?.duration ?: Duration.ZERO
                val newDuration = EffectApi.clampUsingPartialSet(existing, duration)
                effectDuration[event.effect] = Timer(newDuration)
            }
        }
        update()
    }

    private fun update() {
        display = drawDisplay()
    }

    private fun drawDisplay(): MutableList<String> {
        val newDisplay = mutableListOf<String>()
        for ((effect, time) in effectDuration.sorted()) {
            if (time.ended) continue
            if (effect == NonGodPotEffect.INVISIBILITY) continue

            if (effect.isMixin && !config.showMixins) continue

            val remaining = time.remaining.coerceAtLeast(0.seconds)
            val format = remaining.format(TimeUnit.HOUR)
            val color = remaining.timerColor()

            val displayName = effect.displayName
            newDisplay.add("$displayName $color$format")
        }
        val diff = EffectApi.totalEffectsCount - effectDuration.size
        if (diff > 0) {
            newDisplay.add("§eOpen the /effects inventory")
            newDisplay.add("§eto show the missing $diff effects!")
        }
        return newDisplay
    }

    @HandleEvent
    private fun onSecondPassed() {
        if (!isEnabled()) return
        if (!ProfileStorageData.loaded) return

        if (config.displayEnabled) update()

        val effectWarning = config.expireWarning
        val effectSound = config.expireSound

        if (!effectWarning && !effectSound) return

        effectDuration.sorted().forEach { (effect, time) ->
            if (time.remaining.inWholeSeconds != config.expireWarnTime.toLong()) return

            if (effectWarning) {
                TitleManager.sendTitle(effect.displayName)
                ChatUtils.chat("${effect.displayName} §eis running out soon!")
            }
            if (effectSound) repeat(5) { playPlingSound() }
        }
    }


    @HandleEvent
    private fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled() || !config.displayEnabled) return
        if (RiftApi.inRift()) return

        config.position.renderStrings(
            display,
            extraSpace = 3,
            posLabel = "Non God Pot Effects",
        )
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "misc.nonGodPotEffectDisplay", "misc.potionEffect.nonGodPotEffectDisplay")
        event.move(3, "misc.nonGodPotEffectShowMixins", "misc.potionEffect.nonGodPotEffectShowMixins")
        event.move(3, "misc.nonGodPotEffectPos", "misc.potionEffect.nonGodPotEffectPos")
        event.move(95, "misc.potionEffect.nonGodPotEffectPos", "misc.potionEffect.position")
        event.move(95, "misc.potionEffect.nonGodPotEffectDisplay", "misc.potionEffect.displayEnabled")
        event.move(95, "misc.potionEfect.nonGodPotEffectShowMixins", "misc.potionEffect.showMixins")
        event.move(95, "misc.potionEffect", "misc.nonGodPotEffect")
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && !DungeonApi.inDungeon() && !KuudraApi.inKuudra
}
