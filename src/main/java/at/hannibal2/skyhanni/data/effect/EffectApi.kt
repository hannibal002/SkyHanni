package at.hannibal2.skyhanni.data.effect

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.IslandTypeTag
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
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchAll
import at.hannibal2.skyhanni.utils.RegexUtils.matchAllComponents
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.network.chat.Component
import kotlin.time.DurationUnit
import kotlin.time.toDuration

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
        "inventory.effects.filter.colorlesss",
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
        " Repellent: (?<tier>\\w+)?(?: \\((?<time>\\d)s\\))?",
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
     * REGEX-TEST: Time Remaining: Completed!
     * REGEX-TEST: Time Remaining: 1h 2m
     * REGEX-TEST: Time Remaining: 1h 2m 3s
     * REGEX-TEST: Remaining: 1h 2m 3s
     * REGEX-TEST: PAUSED
     */
    private val remainingPattern by RepoPattern.pattern(
        "effects.remaining",
        "^.*Remaining: (?<time>.+)$",
    )
    // </editor-fold>

    init {
        NonGodPotEffect.entries.forEach { it.tabListName }
    }

    private val profileStorage get() = ProfileStorageData.profileSpecific

    // Todo: Add support for poison candy I, and add support for splash / other formats
    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        val msg = event.cleanMessage
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
        if (tabListFooterPattern.matches(msg)) {
            return
        }

        for (effect in NonGodPotEffect.entries) {
            if (effect.effectRemovedPattern?.pattern() == msg) {
                EffectDurationChangeEvent(effect, EffectDurationChangeType.REMOVE, null).post()
                return
            }

            if (effect.effectGainedPattern?.pattern() != msg) continue
            val changeType = effect.effectChangeType ?: continue
            val duration = effect.effectDuration ?: continue

            EffectDurationChangeEvent(effect, changeType, duration).post()
            return
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTabUpdate(event: TablistFooterUpdateEvent) {
        val footerLines = TextHelper.split(event.footer, "\n") ?: listOf(event.footer)
        footerLines.readNonGodPotEffects()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun readEffects(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.ACTIVE_EFFECTS)) return
        godPotTabPattern.firstMatcher(event.lines.map { it.string }) {
            profileStorage?.godPotExpiry = SimpleTimeMark.now() + TimeUtils.getDuration(group("time"))
        }
        event.lines.readNonGodPotEffects()
    }

    private fun List<Component>.readNonGodPotEffects() = tabEffectPattern.matchAllComponents(this) {
        val nonGodPotEffect = NonGodPotEffect.entries.firstOrNull { effect ->
            effect.tablistNamePattern.pattern() == group("effect")
        } ?: return@matchAllComponents
        try {
            val duration = TimeUtils.getDuration(group("time"))
            EffectDurationChangeEvent(nonGodPotEffect, EffectDurationChangeType.SET, duration).post()
        } catch (e: Exception) {
            ChatUtils.debug("Error while reading non god pot effects from tab list! line: '$this'")
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun readPestRepellent(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.PESTS)) return

        repellentPattern.firstMatcher(event.lines.map { it.string }) {
            // Update repellent timer when near expiration to sync with the in-game countdown delay (which is slow)
            val time = group("time")?.toIntOrNull() ?: return@firstMatcher
            val tier = group("tier").uppercase()
            val duration = time.toDuration(DurationUnit.SECONDS)
            val propTier = when (tier) {
                "MAX" -> NonGodPotEffect.PEST_REPELLENT_MAX
                "REGULAR" -> NonGodPotEffect.PEST_REPELLENT
                else -> return@firstMatcher
            }
            EffectDurationChangeEvent(propTier, EffectDurationChangeType.SET, duration).post()
        }
    }

    @HandleEvent(onlyOnIslandTypeTag = [IslandTypeTag.FORAGING_CUSTOM_TREES])
    fun readSalts(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.SALTS)) return
        saltTabPattern.matchAll(event.lines.map { it.string }) {
            val effect = group("effect")
            val duration = TimeUtils.getDuration(group("time"))
            val salt = NonGodPotEffect.entries.firstOrNull {
                it.tablistNamePattern.pattern() == effect
            } ?: return@matchAll
            EffectDurationChangeEvent(salt, EffectDurationChangeType.SET, duration).post()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
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
        cleanName.contains(it.inventoryItemName)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
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
}
