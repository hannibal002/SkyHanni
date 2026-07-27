package at.hannibal2.skyhanni.data.effect

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.TablistFooterUpdateEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.effects.EffectDurationChangeEvent
import at.hannibal2.skyhanni.events.effects.EffectDurationChangeType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchAll
import at.hannibal2.skyhanni.utils.RegexUtils.matchAllComponents
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RegexUtils.replace
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.network.chat.Component
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object EffectApi {

    // <editor-fold desc="Patterns">
    /**
     * REGEX-TEST: God Potion: 4d
     */
    private val godPotTabPattern by RepoPattern.pattern(
        "stats.tabpatterns.godpot-no-color",
        "God Potion: (?<time>[dhms0-9 ]+)",
    )

    /**
     * REGEX-TEST: SCHLURP! The effects of the Hot Chocolate Mixin have been extended by 86h 24m!
     * They will pause if your God Potion expires.
     */
    private val hotChocolateMixinConsumePattern by RepoPattern.pattern(
        "stats.chatpatterns.hotchocolatemixinconsume.colorless",
        ".*Hot Chocolate Mixin have been extended by (?<time>[dhms0-9 ]*)!.*",
    )

    /**
     * REGEX-TEST: GULP! The God Potion grants you powers for 28h 48m!
     * REGEX-TEST: SIP! The God Potion grants you powers for 28h 48m!
     * REGEX-TEST: SLURP! The God Potion grants you powers for 28h 48m!
     */
    private val godPotConsumePattern by RepoPattern.pattern(
        "stats.chatpatterns.godpotconsume.colorless",
        ".*God Potion grants you powers for (?<time>[dhms0-9 ]*)!.*",
    )

    /**
     * REGEX-TEST: (1/2) Active Effects
     */
    private val effectsInventoryPattern by RepoPattern.pattern(
        "inventory.effects",
        "(?:\\(\\d+/\\d+\\) )?Active Effects",
    )

    /**
     * REGEX-TEST: Filter
     */
    private val filterPattern by RepoPattern.pattern(
        "inventory.effects.filter.colorless",
        "Filter",
    )

    /**
     * REGEX-TEST: ▶ God Potion Effects
     */
    private val godPotEffectsFilterSelectPattern by RepoPattern.pattern(
        "inventory.effects.filtergodpotselect.colorless",
        "▶ God Potion Effects",
    )

    /**
     * REGEX-TEST: Remaining: 105:01:34
     */
    private val potionRemainingLoreTimerPattern by RepoPattern.pattern(
        "inventory.effects.effecttimeleft.colorless",
        "Remaining: (?<time>[\\d:]+)",
    )

    /**
     * WRAPPED-REGEX-TEST: " Repellent: MAX (12s)"
     * WRAPPED-REGEX-TEST: " Repellent: Regular (58m)"
     * WRAPPED-REGEX-TEST: " Repellent: Max (58m)"
     */
    private val repellentPattern by RepoPattern.pattern(
        "misc.nongodpot.repellant-no-color",
        "(?: +)?Repellent: (?<tier>\\w+)?(?: \\((?<time>[dhms0-9 ]+)\\))?",
    )

    /**
     * WRAPPED-REGEX-TEST: " Smoldering Polarization I: 58s"
     * WRAPPED-REGEX-TEST: " Wisp's Ice-Flavored Water I: 29m"
     * WRAPPED-REGEX-TEST: "     Mushed Glowy Tonic I 43m"
     * REGEX-TEST: Wisp's Ice-Flavored Water I 10m
     */
    private val tabEffectPattern by RepoPattern.pattern(
        "tab.effects-no-color",
        " *(?<effect>[\\w\\-' ]+ (?<tier>[IVXLC]+)) ?(?:|[: ])+(?<time>[dhms0-9 ]+)",
    )

    /**
     * REGEX-TEST: Lushlilac Bonbon: 12h
     * REGEX-TEST: Prime Lushlilac Bonbon: 18h
     * REGEX-TEST: Prime Lushlilac Bonbon: 17h 58m
     */
    private val saltTabPattern by RepoPattern.pattern(
        "tab.salts-no-color",
        " (?<effect>[\\w\\-' ]+)*: *(?<time>[dhms0-9 ]+)",
    )

    /**
     * WRAPPED-REGEX-TEST: " Press TAB or type /effects to view your active effects!"
     */
    private val tabListFooterPattern by RepoPattern.pattern(
        "tab.footer.effects",
        " Press TAB or type /effects to view your active effects!",
    )

    /**
     * REGEX-TEST: Time Remaining: 1h 2m
     * REGEX-TEST: Time Remaining: 1h 2m 3s
     * REGEX-TEST: Remaining: 1h 2m 3s
     * REGEX-FAIL: Time Remaining: Completed!
     * REGEX-FAIL: PAUSED
     */
    private val remainingPattern by RepoPattern.pattern(
        "effects.remaining",
        ".*Remaining: (?<time>[dhms0-9 ]+)$",
    )
    // </editor-fold>

    init {
        NonGodPotEffect.entries.forEach { it.displayName }
    }

    private val profileStorage get() = ProfileStorageData.profileSpecific

    // Todo: Add support for poison candy I, and add support for splash / other formats
    @HandleEvent(onlyOnSkyblock = true)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        var msg = event.cleanMessage
        hotChocolateMixinConsumePattern.matchMatcher(msg) {
            val durationAdded = TimeUtils.getDuration(group("time"))
            EffectDurationChangeEvent(
                NonGodPotEffect.HOT_CHOCOLATE,
                EffectDurationChangeType.ADD,
                durationAdded,
            ).post()
        }
        godPotConsumePattern.matchMatcher(msg) {
            val durationAdded = TimeUtils.getDuration(group("time"))
            val existingValue = profileStorage?.godPotExpiry?.takeIfInitialized() ?: SimpleTimeMark.now()
            profileStorage?.godPotExpiry = existingValue + durationAdded
        }

        msg = tabListFooterPattern.replace(msg) { "" }.trim()

        for (effect in NonGodPotEffect.entries) {
            if (effect.effectRemovedPattern?.matches(msg) == true) {
                EffectDurationChangeEvent(effect, EffectDurationChangeType.REMOVE, null).post()
                return
            }

            if (effect.effectGainedPattern?.matches(msg) != true) continue
            val changeType = effect.effectChangeType ?: continue
            val duration = effect.effectDuration ?: continue

            EffectDurationChangeEvent(effect, changeType, duration).post()
            return
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onTabUpdate(event: TablistFooterUpdateEvent) {
        val footerLines = TextHelper.split(event.footer, "\n") ?: listOf(event.footer)
        footerLines.readNonGodPotEffects()
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onWidgetUpdate(event: WidgetUpdateEvent) {
        when (event.widget) {
            TabWidget.ACTIVE_EFFECTS -> {
                godPotTabPattern.firstMatcher(event.cleanLines) {
                    profileStorage?.godPotExpiry = SimpleTimeMark.now() + TimeUtils.getDuration(group("time"))
                }
                event.lines.readNonGodPotEffects()
            }
            TabWidget.SALTS -> {
                saltTabPattern.matchAll(event.cleanLines) {
                    val effect = group("effect")
                    val duration = TimeUtils.getDuration(group("time"))
                    val salt = NonGodPotEffect.entries.firstOrNull {
                        it.tablistNamePattern.pattern() == effect
                    } ?: return@matchAll
                    EffectDurationChangeEvent(salt, EffectDurationChangeType.PARTIAL_SET, duration).post()
                }
            }
            TabWidget.PESTS -> {
                repellentPattern.firstMatcher(event.cleanLines) {
                    val timeStr = groupOrNull("time") ?: return@firstMatcher
                    val duration = TimeUtils.getDurationOrNull(timeStr) ?: return@firstMatcher
                    val tier = group("tier").uppercase()
                    val propTier = when (tier) {
                        "MAX" -> NonGodPotEffect.PEST_REPELLENT_MAX
                        "REGULAR" -> NonGodPotEffect.PEST_REPELLENT
                        else -> return@firstMatcher
                    }
                    EffectDurationChangeEvent(propTier, EffectDurationChangeType.PARTIAL_SET, duration).post()
                }
            }
            else -> {}
        }
    }

    private fun List<Component>.readNonGodPotEffects() = tabEffectPattern.matchAllComponents(this) {
        val nonGodPotEffect = NonGodPotEffect.entries.firstOrNull { effect ->
            effect.tablistNamePattern.matches(group("effect"))
        } ?: return@matchAllComponents
        try {
            val duration = TimeUtils.getDuration(group("time"))
            EffectDurationChangeEvent(nonGodPotEffect, EffectDurationChangeType.PARTIAL_SET, duration).post()
        } catch (_: Exception) {
            ChatUtils.debug("Error while reading non god pot effects from tab list! line: '$this'")
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!event.isGodPotEffectsFilterSelect()) return

        val potionLore = event.inventoryItems[10]?.getCleanLore() ?: run {
            // No active god pot effects found, reset the expiry time
            profileStorage?.godPotExpiry = SimpleTimeMark.farPast()
            return
        }

        val expiryDuration = potionRemainingLoreTimerPattern.firstMatcher(potionLore) {
            TimeUtils.getDuration(group("time"))
        } ?: return

        profileStorage?.godPotExpiry = SimpleTimeMark.now() + expiryDuration
    }

    private fun InventoryUpdatedEvent.isGodPotEffectsFilterSelect(): Boolean =
        effectsInventoryPattern.matches(this.inventoryName) &&
            this.inventoryItems.values.firstOrNull {
                filterPattern.matches(it.cleanName)
            }?.getCleanLore()?.any {
                godPotEffectsFilterSelectPattern.matches(it)
            } ?: false

    private fun SafeItemStack.getNonGodPotEffectOrNull(): NonGodPotEffect? = NonGodPotEffect.entries.firstOrNull {
        it.inventoryItemNamePattern.matches(cleanName)
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!effectsInventoryPattern.matches(event.inventoryName)) return

        loop@ for (stack in event.inventoryItems.values) {
            val effect = stack.getNonGodPotEffectOrNull() ?: continue
            val lore = stack.getCleanLore()
            remainingPattern.firstMatcher(lore) {
                val duration = try {
                    TimeUtils.getDuration(group("time"))
                } catch (e: Exception) {
                    ErrorManager.logErrorWithData(
                        e,
                        "Error while reading Non God-Potion effects from inventory",
                        "lore" to lore,
                    )
                    continue@loop
                }
                EffectDurationChangeEvent(effect, EffectDurationChangeType.SET, duration).post()
            }
        }
    }

    /**
     * Applies a PARTIAL_SET update by constraining the existing ticking estimate
     * to the range implied by the displayed precision.
     *
     * Hypixel truncates omitted units:
     * - 1w      -> [1w, 2w)
     * - 1d 5h   -> [1d5h, 1d6h)
     * - 1h 20m  -> [1h20m, 1h21m)
     * - 20m     -> [20m, 21m)
     * - 20s     -> [20s, 21s)
     *
     * The existing estimate is preserved if it falls within the valid range;
     * otherwise it is clamped to the nearest valid value.
     */
    fun clampUsingPartialSet(existing: Duration, duration: Duration): Duration {
        if (existing == Duration.ZERO) {
            return duration
        }

        val upperBound = when {
            duration.inWholeSeconds % 60 != 0L -> duration + 1.seconds
            duration.inWholeMinutes % 60 != 0L -> duration + 1.minutes
            duration.inWholeHours % 24 != 0L -> duration + 1.hours
            duration.inWholeDays % 7 != 0L -> duration + 1.days
            duration.inWholeDays > 0L -> duration + 7.days
            else -> duration + 1.seconds
        }

        return when {
            existing < duration -> duration
            existing >= upperBound -> upperBound - 1.seconds
            else -> existing
        }
    }
}
