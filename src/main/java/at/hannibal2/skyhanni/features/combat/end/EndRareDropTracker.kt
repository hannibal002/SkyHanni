package at.hannibal2.skyhanni.features.combat.end

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.events.EndBoss
import at.hannibal2.skyhanni.events.EndBossFightEndEvent
import at.hannibal2.skyhanni.events.EndLootFoundEvent
import at.hannibal2.skyhanni.events.GolemWeightEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.features.combat.end.dragon.DragonFightState
import at.hannibal2.skyhanni.features.combat.end.dragon.DragonWeight
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ClipboardUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyClicked
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.decorators.TitledFrameRenderable.Companion.withTitledFrame
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import com.google.gson.annotations.Expose
import net.minecraft.client.gui.screens.ChatScreen
import kotlin.time.Duration.Companion.seconds

/**
 * Counts the truly rare End island drops and how long the current dry streak is.
 *
 * A fight only counts towards the streak when its weight was high enough to be eligible for the
 * drop at all - a leeched kill never had a chance and would otherwise make the streak look worse
 * than it is.
 */
@SkyHanniModule
object EndRareDropTracker {

    private val config get() = SkyHanniMod.feature.combat.endIsland.rareDropTracker

    /** Below these weights the fight cannot roll the drop, so it does not count as a dry one. */
    private const val DRAGON_WEIGHT_REQUIREMENT = 450.0
    private const val GOLEM_WEIGHT_REQUIREMENT = 250.0

    /** Per summoning eye placed, taken from the drop tables. */
    private const val LEGENDARY_PET_CHANCE_PER_EYE = 0.01
    private const val EPIC_PET_CHANCE_PER_EYE = 0.05

    /** Per protector kill, before Magic Find and Pet Luck are applied. */
    private const val TIER_BOOST_CHANCE = 0.2

    private const val LINE_SPACING = 3
    private const val ICON_SPACING = 4

    private val LEGENDARY_PET = "ENDER_DRAGON;4".toInternalName()
    private val EPIC_PET = "ENDER_DRAGON;3".toInternalName()
    private val TIER_BOOST_CORE = "PET_ITEM_TIER_BOOST_DROP".toInternalName()

    class Data {
        @Expose var legendaryPets: Int = 0
        @Expose var epicPets: Int = 0
        @Expose var tierBoostCores: Int = 0

        /** Eligible fights since the last drop of the matching kind. */
        @Expose var dryDragons: Int = 0
        @Expose var dryGolems: Int = 0

        /** Summed drop chance of those fights, in percent. */
        @Expose var dryDragonChance: Double = 0.0
        @Expose var dryGolemChance: Double = 0.0
    }

    private val storage: Data? get() = ProfileStorageData.profileSpecific?.endRareDrops

    /**
     * Everything the overlay shows. It is only rebuilt when this changes, instead of building new
     * renderables every frame for counters that move a few times per hour. The fresh gains belong
     * in here too, so the green number appears and disappears on its own.
     */
    private data class State(
        val showDragon: Boolean,
        val showGolem: Boolean,
        val showDryStreak: Boolean,
        val legendaryPets: Int,
        val epicPets: Int,
        val tierBoostCores: Int,
        val dryDragons: Int,
        val dryGolems: Int,
        val dryDragonChance: Double,
        val dryGolemChance: Double,
        val dragonGain: Double?,
        val golemGain: Double?,
    )

    private var state: State? = null
    private var display: Renderable? = null

    /** How long a fresh gain stays appended to its line. */
    private val GAIN_DURATION = 5.seconds

    /**
     * What the last fight added to the streak, per boss. Shown briefly so the change is visible -
     * the total alone moves too little to notice.
     */
    private val recentGain = mutableMapOf<EndBoss, Double>()
    private val gainedAt = mutableMapOf<EndBoss, SimpleTimeMark>()

    private fun rememberGain(boss: EndBoss, amount: Double) {
        recentGain[boss] = amount
        gainedAt[boss] = SimpleTimeMark.now()
    }

    private fun freshGain(boss: EndBoss): Double? {
        val at = gainedAt[boss] ?: return null
        if (at.passedSince() > GAIN_DURATION) return null
        return recentGain[boss]
    }

    /**
     * Hypixel refreshes the stats a moment after the kill, so reading them right away would use
     * the values from before any drop related buff was applied. Waiting is more reliable than
     * capturing early and hoping the values were already current.
     */
    private val STAT_DELAY = 2.seconds

    private fun magicFind() = SkyblockStat.MAGIC_FIND.lastKnownValue ?: 0.0
    private fun petLuck() = SkyblockStat.PET_LUCK.lastKnownValue ?: 0.0

    private fun withPetBonus(baseChance: Double) = baseChance * (1 + (magicFind() + petLuck()) / 100)

    /** Pet Luck only applies to pets, and the Tier Boost Core is a pet item rather than a pet. */
    private fun withMagicFindBonus(baseChance: Double) = baseChance * (1 + magicFind() / 100)

    /**
     * The base chance is fixed at the kill, the bonus is read once the stats have caught up.
     * Whether the fight counted at all is decided immediately - only the number arrives late.
     *
     * [data] is handed in rather than looked up again later: the fight belongs to the profile it
     * was fought on, and the counter has already been raised there. Looking it up again could
     * find nothing - after leaving the island, say - and leave a counted fight without its
     * chance. Leaving does not cancel anything; the queue runs regardless of where the player is.
     */
    private fun addChanceLater(boss: EndBoss, data: Data, baseChance: Double) {
        DelayedRun.runDelayed(STAT_DELAY, "EndRareDropTracker chance") {
            val gain = if (boss == EndBoss.DRAGON) withPetBonus(baseChance) else withMagicFindBonus(baseChance)
            if (boss == EndBoss.DRAGON) data.dryDragonChance += gain else data.dryGolemChance += gain
            rememberGain(boss, gain)
        }
    }

    @HandleEvent
    private fun onEndLootFound(event: EndLootFoundEvent) {
        val data = storage ?: return
        when (event.internalName) {
            LEGENDARY_PET -> {
                data.legendaryPets++
                resetDragonStreak(data)
            }
            EPIC_PET -> {
                data.epicPets++
                resetDragonStreak(data)
            }
            TIER_BOOST_CORE -> {
                data.tierBoostCores++
                data.dryGolems = 0
                data.dryGolemChance = 0.0
            }
        }
    }

    private fun resetDragonStreak(data: Data) {
        data.dryDragons = 0
        data.dryDragonChance = 0.0
    }

    /**
     * Counted on the fight result rather than on the kill, because only then is the weight known.
     */
    @HandleEvent
    private fun onEndBossFightEnd(event: EndBossFightEndEvent) {
        // The protector is counted in onGolemWeight instead: its weight needs the zealot count,
        // which only arrives after this event.
        if (event.boss != EndBoss.DRAGON) return
        val data = storage ?: return
        val weight = DragonWeight.calculateWeight(
            DragonFightState.eyesPlaced,
            event.place,
            event.topDamage,
            event.yourDamage,
        )
        if (weight < DRAGON_WEIGHT_REQUIREMENT) return
        data.dryDragons++
        // Read now: the eye count is cleared as soon as the summary has been handled.
        val baseChance = DragonFightState.eyesPlaced * (LEGENDARY_PET_CHANCE_PER_EYE + EPIC_PET_CHANCE_PER_EYE)
        addChanceLater(EndBoss.DRAGON, data, baseChance)
    }

    @HandleEvent
    private fun onGolemWeight(event: GolemWeightEvent) {
        val data = storage ?: return
        if (event.weight < GOLEM_WEIGHT_REQUIREMENT) return
        data.dryGolems++
        addChanceLater(EndBoss.END_STONE_PROTECTOR, data, TIER_BOOST_CHANCE)
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onTick() {
        if (!config.enabled) return
        val newState = storage?.let { currentState(it) }
        if (newState == state) return
        state = newState
        val lines = newState?.let { buildLines(it) }.orEmpty()
        display = if (lines.isEmpty()) null else {
            Renderable.vertical(lines, spacing = LINE_SPACING).withTitledFrame(Renderable.text("§f§lRare Drops"))
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onRender(event: GuiRenderEvent) {
        if (!config.enabled) return
        val renderable = display ?: return
        config.position.renderRenderable(renderable, posLabel = "End Rare Drops")
    }

    private fun currentState(data: Data) = State(
        showDragon = config.showDragon,
        showGolem = config.showGolem,
        showDryStreak = config.showDryStreak,
        legendaryPets = data.legendaryPets,
        epicPets = data.epicPets,
        tierBoostCores = data.tierBoostCores,
        dryDragons = data.dryDragons,
        dryGolems = data.dryGolems,
        dryDragonChance = data.dryDragonChance,
        dryGolemChance = data.dryGolemChance,
        dragonGain = freshGain(EndBoss.DRAGON),
        golemGain = freshGain(EndBoss.END_STONE_PROTECTOR),
    )

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shcopyenddrops") {
            description = "Copy the End rare drop counters to the clipboard."
            category = CommandCategory.USERS_ACTIVE
            simpleCallback { copySummary() }
        }
    }

    /**
     * Clicking the overlay only works while a screen is open, so the world side needs a command.
     */
    private fun copySummary() {
        val data = storage ?: return
        val text = buildString {
            append("Legendary Ender Dragon: ${data.legendaryPets}, ")
            append("Epic Ender Dragon: ${data.epicPets}, ")
            append("Tier Boost Core: ${data.tierBoostCores} | ")
            append("Since Dragons: ${data.dryDragons} (${data.dryDragonChance.roundTo(2)}%), ")
            append("Since Golems: ${data.dryGolems} (${data.dryGolemChance.roundTo(2)}%)")
        }
        copyLine(text)
    }

    /** Each boss forms its own block, so switching one off simply shrinks the overlay. */
    private fun buildLines(state: State): List<Renderable> = buildList {
        if (state.showDragon) {
            add(countLine(LEGENDARY_PET, "§6Legendary Ender Dragon", state.legendaryPets))
            add(countLine(EPIC_PET, "§5Epic Ender Dragon", state.epicPets))
            if (state.showDryStreak) {
                add(dryLine("Since Pet", state.dryDragons, state.dryDragonChance, state.dragonGain))
            }
        }
        if (state.showGolem) {
            add(countLine(TIER_BOOST_CORE, "§6Tier Boost Core", state.tierBoostCores))
            if (state.showDryStreak) {
                add(dryLine("Since Core", state.dryGolems, state.dryGolemChance, state.golemGain))
            }
        }
    }

    /** [name] reads as "kills since the last one", so the number needs no further wording. */
    private fun dryLine(name: String, count: Int, chance: Double, gain: Double?): Renderable {
        val text = "$name: $count (${chance.roundTo(2)}%)"
        // The gain is deliberately left out of the copied text - it is a moment, not a statistic.
        val gainText = gain?.let { " §a(+${it.roundTo(2)}%)" }.orEmpty()
        return copyable(
            Renderable.text("§7$name: §f$count §8(${chance.roundTo(2)}%)$gainText"),
            text,
        )
    }

    private fun countLine(internalName: NeuInternalName, label: String, count: Int): Renderable {
        val line = Renderable.horizontal(
            listOf(Renderable.item(internalName), Renderable.text("$label§7: §f$count")),
            spacing = ICON_SPACING,
        )
        return copyable(line, "${label.removeColor()}: $count")
    }

    /**
     * Right click copies the line as plain text, ready to be pasted into chat.
     *
     * The framework's own [Renderable.clickable] cannot be used here: it refuses every click made
     * outside an inventory, which rules out the chat screen - the one place where the mouse is
     * free while the overlay is on screen. Hover and click are therefore handled directly.
     */
    private fun copyable(line: Renderable, text: String): Renderable = object : Renderable {
        override val width get() = line.width
        override val height get() = line.height
        override val horizontalAlign get() = line.horizontalAlign
        override val verticalAlign get() = line.verticalAlign

        override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
            val hovered = MinecraftCompat.screen is ChatScreen &&
                isHovered(mouseOffsetX, mouseOffsetY)
            if (hovered && KeyboardManager.RIGHT_MOUSE.isKeyClicked()) {
                copyLine(text)
            }
            line.render(mouseOffsetX, mouseOffsetY)
        }
    }

    private fun copyLine(text: String) {
        ClipboardUtils.copyToClipboard(text)
        ChatUtils.chat("§eCopied: §f$text")
    }
}
