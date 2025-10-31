package at.hannibal2.hanni.features.misc.effects

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.data.ProfileStorageData
import at.hannibal2.hanni.data.effect.NonGodPotEffect
import at.hannibal2.hanni.data.title.TitleManager
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.ProfileJoinEvent
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.events.TablistFooterUpdateEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.events.effects.EffectDurationChangeEvent
import at.hannibal2.hanni.events.effects.EffectDurationChangeType
import at.hannibal2.hanni.features.dungeon.DungeonApi
import at.hannibal2.hanni.features.nether.kuudra.KuudraApi
import at.hannibal2.hanni.features.rift.RiftApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.RenderUtils.renderStrings
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.SoundUtils.playPlingSound
import at.hannibal2.hanni.utils.TimeUnit
import at.hannibal2.hanni.utils.TimeUtils.format
import at.hannibal2.hanni.utils.TimeUtils.timerColor
import at.hannibal2.hanni.utils.Timer
import at.hannibal2.hanni.utils.collection.CollectionUtils.sorted
import at.hannibal2.hanni.utils.collection.TimeLimitedSet
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@HanniModule
object NonGodPotEffectDisplay {

    private val config get() = HanniMod.feature.misc.nonGodPotEffect
    private var checkFooter = false
    private val effectDuration = mutableMapOf<NonGodPotEffect, Timer>()
    private val setRecently: TimeLimitedSet<NonGodPotEffect> = TimeLimitedSet(5.seconds)
    private var display = emptyList<String>()

    /**
     * REGEX-TEST: §7You have §e10 §7non-god effects.
     */
    private val effectsCountPattern by RepoPattern.pattern(
        "misc.nongodpot.effects",
        "§7You have §e(?<name>\\d+) §7non-god effects\\.",
    )
    private var totalEffectsCount = 0

    @HandleEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        effectDuration.clear()
        display = emptyList()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: HanniChatEvent) {
        if (event.message == "§aYou cleared all of your active effects!") {
            effectDuration.clear()
            update()
        }
    }

    @HandleEvent
    fun onEffectUpdate(event: EffectDurationChangeEvent) {
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
        }
        update()
    }

    private fun update() {
        if (effectDuration.values.removeIf { it.ended }) {
            // to fetch the real amount of active pots
            totalEffectsCount = 0
            checkFooter = true
        }

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

            val displayName = effect.tabListName
            newDisplay.add("$displayName $color$format")
        }
        val diff = totalEffectsCount - effectDuration.size
        if (diff > 0) {
            newDisplay.add("§eOpen the /effects inventory")
            newDisplay.add("§eto show the missing $diff effects!")
            checkFooter = true
        }
        return newDisplay
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        if (!ProfileStorageData.loaded) return

        if (config.displayEnabled) update()

        val effectWarning = config.expireWarning
        val effectSound = config.expireSound

        if (!effectWarning && !effectSound) return

        effectDuration.sorted().forEach { (effect, time) ->
            if (time.remaining.inWholeSeconds != config.expireWarnTime.toLong()) return

            if (effectWarning) TitleManager.sendTitle(effect.tabListName)
            if (effectSound) repeat(5) { playPlingSound() }
        }
    }

    @HandleEvent
    fun onWorldChange() {
        checkFooter = true
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTabUpdate(event: TablistFooterUpdateEvent) {
        if (!checkFooter) return
        val lines = event.footer.split("\n")
        if (!lines.any { it.contains("§a§lActive Effects") }) return

        checkFooter = false
        var effectsCount = 0
        for (line in lines) {
            effectsCountPattern.matchMatcher(line) {
                val group = group("name")
                effectsCount = group.toInt()
            }
        }
        totalEffectsCount = effectsCount
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled() || !config.displayEnabled) return
        if (RiftApi.inRift()) return

        config.position.renderStrings(
            display,
            extraSpace = 3,
            posLabel = "Non God Pot Effects",
        )
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
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
