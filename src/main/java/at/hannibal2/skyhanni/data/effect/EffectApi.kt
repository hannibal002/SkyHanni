package at.hannibal2.skyhanni.data.effect

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
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
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchAll
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@SkyHanniModule
object EffectApi {

    // <editor-fold desc="Patterns">
    /**
     * REGEX-TEST: §cGod Potion§f: 4d
     */
    private val godPotTabPattern by RepoPattern.pattern(
        "stats.tabpatterns.godpot",
        "(?:§.)*God Potion(?:§.)*: (?:§.)*(?<time>[dhms0-9 ]+)(?:§.)*",
    )

    /**
     * REGEX-TEST: §a§lSCHLURP! §r§eThe effects of the §r§9Hot Chocolate Mixin §r§ehave been extended by §r§986h 24m§r§e!
     * They will pause if your §r§cGod Potion §r§eexpires.
     */
    private val hotChocolateMixinConsumePattern by RepoPattern.pattern(
        "stats.chatpatterns.hotchocolatemixinconsume",
        "(?:§.)+.*(?:§.)+Hot Chocolate Mixin ?(?:§.)+.*extended by (?:§.)+(?<time>[dhms0-9 ]*)(?:§.)+!.*",
    )

    /**
     * REGEX-TEST: §a§lGULP! §r§eThe §r§cGod Potion §r§egrants you powers for §r§928h 48m§r§e!
     * REGEX-TEST: §a§lSIP! §r§eThe §r§cGod Potion §r§egrants you powers for §r§928h 48m§r§e!
     * REGEX-TEST: §a§lSLURP! §r§eThe §r§cGod Potion §r§egrants you powers for §r§928h 48m§r§e!
     */
    private val godPotConsumePattern by RepoPattern.pattern(
        "stats.chatpatterns.godpotconsume",
        "(?:§.)+.*(?:§.)+God Potion ?(?:§.)+.*grants you powers for (?:§.)+(?<time>[dhms0-9 ]*)(?:§.)+!.*",
    )

    /**
     * REGEX-TEST: (1/2) Active Effects
     */
    private val effectsInventoryPattern by RepoPattern.pattern(
        "inventory.effects",
        "(?:§.)?(?:[(\\d\\/)]* )?Active Effects",
    )

    /**
     * REGEX-TEST: §aFilter
     */
    private val filterPattern by RepoPattern.pattern(
        "inventory.effects.filter",
        "§aFilter",
    )

    /**
     * REGEX-TEST: §b▶ God Potion Effects
     */
    private val godPotEffectsFilterSelectPattern by RepoPattern.pattern(
        "inventory.effects.filtergodpotselect",
        "§b▶ God Potion Effects",
    )

    /**
     * REGEX-TEST: §7Remaining: §f105:01:34
     */
    private val potionRemainingLoreTimerPattern by RepoPattern.pattern(
        "inventory.effects.effecttimeleft",
        "§7Remaining: §f(?<time>[\\d:]+)",
    )

    /**
     * REGEX-TEST:  Repellent: §r§9MAX §r§7(12s)
     */
    private val repellentPattern by RepoPattern.pattern(
        "misc.nongodpot.repellant",
        " Repellent: §r§[97a](?<tier>\\w+)?(?: §r§7\\((?<time>\\d)s\\))?",
    )

    /**
     * REGEX-TEST:  §r§aSmoldering Polarization I§r§f: 58s
     * REGEX-TEST:  §r§bWisp's Ice-Flavored Water I§r§f: 29m
     * REGEX-TEST:      §2Mushed Glowy Tonic I §r§f43m
     * REGEX-TEST: §r§r§bWisp's Ice-Flavored Water I§r§r§r §r§f10m§r
     */
    private val tabEffectPattern by RepoPattern.pattern(
        "tab.effects",
        " *(?:§.)*(?<effect>§.[\\w\\-' ]+ (?<tier>[IVXLC]+)) ?(?:§.|[: ])+(?<time>[dhms0-9 ]+)(?:§.)*"
    )

    /**
     * REGEX-TEST: §r§5Lushlilac Bonbon§r§f: §r§a12h
     * REGEX-TEST: §r§5Prime Lushlilac Bonbon§r§f: §r§a18h
     * REGEX-TEST: §r§5Prime Lushlilac Bonbon§r§f: §r§a17h 58m
     */
    private val saltTabPattern by RepoPattern.pattern(
        "tab.salts",
        " (?<effect>(?:§.)*[\\w\\-' ]+(?:§.)*)*: *(?:§.)*(?<time>[dhms0-9 ]+)"
    )
    // </editor-fold>

    private val profileStorage get() = ProfileStorageData.profileSpecific

    // Todo : cleanup and add support for poison candy I, and add support for splash / other formats
    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent) {
        hotChocolateMixinConsumePattern.matchMatcher(event.message) {
            val durationAdded = TimeUtils.getDuration(group("time"))
            EffectDurationChangeEvent(
                NonGodPotEffect.HOT_CHOCOLATE,
                EffectDurationChangeType.ADD,
                durationAdded,
            ).post()
        }
        godPotConsumePattern.matchMatcher(event.message) {
            val durationAdded = TimeUtils.getDuration(group("time"))
            val existingValue = profileStorage?.godPotExpiry?.takeIfInitialized() ?: SimpleTimeMark.now()
            profileStorage?.godPotExpiry = existingValue + durationAdded
        }

        val effect: NonGodPotEffect?
        val changeType: EffectDurationChangeType?
        val duration: Duration?

        val modifiedMessage = event.message.replace(
            " Press TAB or type /effects to view your active effects!",
            ""
        )

        when (modifiedMessage) {
            "§aYou ate a §r§aRe-heated Gummy Polar Bear§r§a!" -> {
                effect = NonGodPotEffect.SMOLDERING
                changeType = EffectDurationChangeType.ADD // These stack when you consume them
                duration = 1.hours
            }
            "§a§lBUFF! §fYou have gained §r§2Mushed Glowy Tonic I§r§f!" -> {
                effect = NonGodPotEffect.GLOWY
                changeType = EffectDurationChangeType.SET
                duration = 1.hours
            }
            "§a§lBUFF! §fYou splashed yourself with §r§bWisp's Ice-Flavored Water I§r§f!" -> {
                effect = NonGodPotEffect.WISP
                changeType = EffectDurationChangeType.SET
                duration = 5.minutes
            }
            "§eYou consumed a §r§fGreat Spook Potion§r§e!" -> {
                effect = NonGodPotEffect.GREAT_SPOOK
                changeType = EffectDurationChangeType.SET
                duration = 24.hours
            }
            "§a§lBUFF! §fYou have gained §r§6Harvest Harbinger V§r§f!" -> {
                effect = NonGodPotEffect.HARVEST_HARBINGER
                changeType = EffectDurationChangeType.SET
                duration = 25.minutes
            }
            "§a§lYUM! §r§2Pests §r§7will now spawn §r§a2x §r§7less while you break crops for the next §r§a60m§r§7!" -> {
                effect = NonGodPotEffect.PEST_REPELLENT
                changeType = EffectDurationChangeType.SET
                duration = 1.hours
            }
            "§a§lYUM! §r§2Pests §r§7will now spawn §r§a4x §r§7less while you break crops for the next §r§a60m§r§7!" -> {
                effect = NonGodPotEffect.PEST_REPELLENT_MAX
                changeType = EffectDurationChangeType.SET
                duration = 1.hours
            }
            "§e[NPC] §6King Yolkar§f: §rThis egg will help me stomach my pain." -> {
                effect = NonGodPotEffect.GOBLIN
                changeType = EffectDurationChangeType.SET
                duration = 20.minutes
            }
            "§cThe Goblin King's §r§afoul stench §r§chas dissipated!" -> {
                effect = NonGodPotEffect.GOBLIN
                changeType = EffectDurationChangeType.REMOVE
                duration = null
            }
            "§a§lBUFF! §fYou have gained §r§eDouce Pluie de Stinky Cheese I§r§f!" -> {
                effect = NonGodPotEffect.DOUCE_PLUIE_DE_STINKY_CHEESE
                changeType = EffectDurationChangeType.SET
                duration = 1.hours
            }
            else -> return
        }

        EffectDurationChangeEvent(effect, changeType, duration).post()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTabUpdate(event: TablistFooterUpdateEvent) {
        val footerLines = event.footer.split("\n")
        footerLines.readNonGodPotEffects()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun readEffects(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.ACTIVE_EFFECTS)) return
        godPotTabPattern.firstMatcher(event.lines) {
            profileStorage?.godPotExpiry = SimpleTimeMark.now() + TimeUtils.getDuration(group("time"))
        }
        event.lines.readNonGodPotEffects()
    }

    private fun List<String>.readNonGodPotEffects() = tabEffectPattern.matchAll(this) {
        val nonGodPotEffect = NonGodPotEffect.entries.firstOrNull {
            it.tabListName == group("effect")
        } ?: return@matchAll
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

        repellentPattern.firstMatcher(event.lines) {
            // Update repellent timer when near expiration to sync with the in-game countdown delay (which is slow)
            val time = group("time")?.toIntOrNull() ?: return@firstMatcher
            val tier = group("tier")
            val duration = time.toDuration(DurationUnit.SECONDS)
            val propTier = when (tier) {
                "MAX" -> NonGodPotEffect.PEST_REPELLENT_MAX
                "REGULAR" -> NonGodPotEffect.PEST_REPELLENT
                else -> return@firstMatcher
            }
            EffectDurationChangeEvent(propTier, EffectDurationChangeType.SET, duration).post()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GALATEA)
    fun readSalts(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.SALTS)) return
        saltTabPattern.firstMatcher(event.lines) {
            val effect = group("effect")
            val duration = TimeUtils.getDuration(group("time"))
            val salt = NonGodPotEffect.entries.firstOrNull {
                it.tabListName == effect
            } ?: return@firstMatcher
            EffectDurationChangeEvent(salt, EffectDurationChangeType.SET, duration).post()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!event.isGodPotEffectsFilterSelect()) return

        val potionLore = event.inventoryItems[10]?.getLore() ?: run {
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
                filterPattern.matches(it.displayName)
            }?.getLore()?.any {
                godPotEffectsFilterSelectPattern.matches(it)
            } ?: false

    private fun ItemStack.getNonGodPotEffectOrNull(): NonGodPotEffect? = NonGodPotEffect.entries.firstOrNull {
        displayName.contains(it.inventoryItemName)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!event.inventoryName.endsWith("Active Effects")) return

        for (stack in event.inventoryItems.values) {
            val effect = stack.getNonGodPotEffectOrNull() ?: continue
            for (line in stack.getLore()) {
                if (!line.contains("Remaining") || line == "§7Time Remaining: §aCompleted!" || line.contains("Remaining Uses")) continue
                val duration = try {
                    TimeUtils.getDuration(line.split("§f")[1])
                } catch (e: IndexOutOfBoundsException) {
                    ErrorManager.logErrorWithData(
                        e, "Error while reading Non God-Potion effects from tab list",
                        "line" to line,
                    )
                    continue
                }
                EffectDurationChangeEvent(effect, EffectDurationChangeType.SET, duration).post()
            }
        }
    }
}
