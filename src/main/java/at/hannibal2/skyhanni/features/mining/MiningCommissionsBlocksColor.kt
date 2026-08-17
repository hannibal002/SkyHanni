package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.MiningApi.inCrystalHollows
import at.hannibal2.skyhanni.data.MiningApi.inDwarvenMines
import at.hannibal2.skyhanni.data.MiningApi.inGlacite
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.mining.MiningCommissionsBlocksColor.CommissionBlock.Companion.onColor
import at.hannibal2.skyhanni.features.mining.OreType.Companion.isOreType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ConditionalUtils.onToggle
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.firstComponentMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.collection.TimeLimitedSet
import at.hannibal2.skyhanni.utils.compat.ColoredBlockCompat
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.item.DyeColor
import net.minecraft.world.level.block.state.BlockState
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object MiningCommissionsBlocksColor {

    private val config get() = SkyHanniMod.feature.mining.commissionsBlocksColor

    private var enabled = false
    private var active = false

    private val patternGroup = RepoPattern.group("mining.commissions")

    init {
        CommissionBlock.entries.forEach { it.commissionPattern }
    }

    /**
     * REGEX-TEST: CITRINE GEMSTONE COLLECTOR Commission Complete! Visit the King to claim your rewards!
     */
    private val commissionCompletePattern by patternGroup.pattern(
        "complete.colorless",
        "(?<name>.*) Commission Complete! Visit the King to claim your rewards!",
    )

    private var color = DyeColor.RED

    private fun glass(state: BlockState, result: Boolean): BlockState {
        val newColor = if (result) color else DyeColor.GRAY
        return ColoredBlockCompat.fromMeta(newColor.id).createGlassBlockState(state)
    }


    private fun block(result: Boolean): BlockState {
        val newColor = if (result) color else DyeColor.GRAY
        return ColoredBlockCompat.fromMeta(newColor.id).createWoolBlockState()
    }

    private var oldSneakState = false
    private var dirty = false
    private val replaceBlocksMapCache = mutableMapOf<BlockState, BlockState>()

    // TODO Commission API
    @HandleEvent(onlyOnIslandTypeTag = [ADVANCED_MINING])
    private fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.COMMISSIONS)) return
        for (block in CommissionBlock.entries) {
            val shouldHighlight = block.commissionPattern.firstComponentMatcher(event.widget.lines) {
                groupOrNull("done") == null
            } ?: continue
            if (block.highlight != shouldHighlight) {
                if (shouldHighlight && block in ignoredTabListCommissions) continue
                block.highlight = shouldHighlight
                dirty = true
            }
        }
    }

    private val ignoredTabListCommissions = TimeLimitedSet<CommissionBlock>(5.seconds)

    // TODO Commission API
    @HandleEvent(onlyOnIslandTypeTag = [ADVANCED_MINING])
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!enabled) return
        commissionCompletePattern.matchMatcher(event.cleanMessage) {
            val name = group("name")
            val block = CommissionBlock.entries.find { it.commissionName.equals(name, ignoreCase = true) } ?: return
            block.highlight = false
            dirty = true
            ignoredTabListCommissions.add(block)
        }
    }

    @HandleEvent
    private fun onTick() {
        val newEnabled = (inCrystalHollows || inGlacite) && config.enabled
        var reload = false
        if (newEnabled != enabled) {
            enabled = newEnabled
            reload = true
            if (enabled) {
                active = true
            }
        }

        if (enabled) {
            if (config.sneakQuickToggle.get()) {
                val sneaking = PlayerUtils.isSneaking()
                if (sneaking != oldSneakState) {
                    oldSneakState = sneaking
                    if (oldSneakState) {
                        active = !active
                        dirty = true
                    }
                }
            }
            if (dirty) {
                reload = true
            }
        }

        if (reload) {
            replaceBlocksMapCache.clear()
            MinecraftCompat.reloadChunks()
            dirty = false
        }
    }

    @HandleEvent
    private fun onConfigLoad(event: ConfigLoadEvent) {
        color = config.color.get().toDyeColor()
        config.sneakQuickToggle.onToggle {
            oldSneakState = false
            if (!active) {
                active = true
                dirty = true
            }
        }
        config.color.onToggle {
            color = config.color.get().toDyeColor()
            dirty = true
        }
    }

    @HandleEvent
    private fun onWorldChange() {
        enabled = false
        replaceBlocksMapCache.clear()
    }

    @HandleEvent
    private fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Mining Commissions Blocks Color")
        if (!enabled) {
            event.addIrrelevant("not enabled")
            return
        }

        event.addData {
            add("inDwarvenMines: $inDwarvenMines")
            add("inCrystalHollows: $inCrystalHollows")
            add("inGlaciteArea: $inGlacite")
            add("active: $active")
        }
    }

    enum class CommissionBlock(
        val commissionName: String,
        val oreType: OreType,
        var highlight: Boolean = false,
    ) {
        // Dwarven Mines
        MITHRIL(
            "Mithril Everywhere",
            OreType.MITHRIL,
        ),

        // Crystal Hollows
        AMBER(
            "Amber Gemstone Collector",
            OreType.AMBER,
        ),
        TOPAZ(
            "Topaz Gemstone Collector",
            OreType.TOPAZ,
        ),
        AMETHYST(
            "Amethyst Gemstone Collector",
            OreType.AMETHYST,
        ),
        RUBY(
            "Ruby Gemstone Collector",
            OreType.RUBY,
        ),
        JADE(
            "Jade Gemstone Collector",
            OreType.JADE,
        ),
        SAPPHIRE(
            "Sapphire Gemstone Collector",
            OreType.SAPPHIRE,
        ),

        // Glacite Tunnels
        GLACITE(
            "Glacite Collector",
            OreType.GLACITE,
        ),
        UMBER(
            "Umber Collector",
            OreType.UMBER,
        ),
        TUNGSTON(
            "Tungsten Collector",
            OreType.TUNGSTEN,
        ),
        PERIDOT(
            "Peridot Gemstone Collector",
            OreType.PERIDOT,
        ),
        AQUAMARINE(
            "Aquamarine Gemstone Collector",
            OreType.AQUAMARINE,
        ),
        CITRINE(
            "Citrine Gemstone Collector",
            OreType.CITRINE,
        ),
        ONYX(
            "Onyx Gemstone Collector",
            OreType.ONYX,
        ),
        ;

        val commissionPattern by patternGroup.pattern(
            "commission",
            " $commissionName: .*?(?<done>DONE)?"
        )

        companion object {
            fun CommissionBlock.onColor(state: BlockState): BlockState =
                if (oreType.isGemstone()) glass(state, highlight) else block(highlight)
        }
    }

    fun processState(state: BlockState?): BlockState? {
        if (!enabled || !active) return state
        if (state == null) return null
        try {
            return replaceBlocksMapCache.getOrPut(state) {
                CommissionBlock.entries.firstOrNull {
                    state.isOreType(it.oreType)
                }?.onColor(state) ?: state
            }
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Error in MiningCommissionsBlocksColor")
            return state
        }
    }
}
