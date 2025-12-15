package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.GardenJson
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHoeExp
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHoeLevel
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getItemUuid
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.appendString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object HoeLevelDisplay {

    val pos = Position(100, 100)
    private var hoeLevels: List<Int>? = null
    private var hoeOverflow = 200000
    private var display: List<Renderable>? = null
    private val gardenStorage get() = GardenApi.storage
    private val config get() = SkyHanniMod.feature.garden.hoeLevelDisplay

    private val patternGroup = RepoPattern.group("hoe.levels")

    /**
     * REGEX-TEST: §3§lOVERFLOW! §r§7Your §r§5Turing Sugar Cane Hoe Mk. III §r§7has just dropped a §r§9Tool Exp Capsule§r§7!
     */
    val levelUpPattern by patternGroup.pattern(
        "levelup",
        "§3§lOVERFLOW! §r§7Your (?:§.)+(?<tool>.*) §r§7has just dropped a §r§9Tool Exp Capsule§r§7!",
    )

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onTick(event: SkyHanniTickEvent) {
        if (!isEnabled()) return
        display = null
        val list = mutableListOf<Renderable>()
        list.add(Renderable.text("§6Hoe Levels"))
        val heldItem = InventoryUtils.getItemInHand()
        val hoeExp = heldItem?.getHoeExp() ?: return
        var hoeLevel = heldItem.getHoeLevel() ?: return
        var next = hoeOverflow
        val hoeLevels = hoeLevels ?: return

        if (hoeLevel <= hoeLevels.size) {
            next = hoeLevels.let { it[hoeLevel - 1] }
        }

        if (hoeLevel > hoeLevels.size && config.overflow) {
            val uuid = heldItem.getItemUuid()
            val overflowLevel = getOverflowHoeLevel(uuid)
            if (overflowLevel != null) {
                hoeLevel += overflowLevel
            }
        }
        list.add(Renderable.text("§7Level §8$hoeLevel➜§3${hoeLevel + 1}"))

        var colorPrefix = "§e"
        if (hoeExp > next) {
            colorPrefix = "§c§l"
            if (hoeLevel >= 40) list.add(Renderable.text("§3§lOVERCLOCK REQUIRED!"))
            else list.add(Renderable.text("§c§lUPGRADE REQUIRED!"))
        }
        val formattedXp = hoeExp.addSeparators()
        val formattedXpToNext = next.addSeparators()
        list.add(Renderable.text("$colorPrefix$formattedXp§8/§e$formattedXpToNext"))

        display = list
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onChat(event: SkyHanniChatEvent) {
        levelUpPattern.matchMatcher(event.message) {
            val heldItem = InventoryUtils.getItemInHand() ?: return
            val leveledUpTool = group("tool")
            val heldItemName = heldItem.displayName.removeColor()
            if (!heldItemName.contains(leveledUpTool)) return
            val overflowLevel = addOverflowHoeLevel(heldItem.getItemUuid())
            if (isEnabled() && config.overflow && overflowLevel != null) {
                val currentLevel = heldItem.getHoeLevel() ?: return
                event.chatComponent = event.chatComponent.appendString(" §8(§3Level ${currentLevel + overflowLevel}§8)")
            }
        }
    }

    private fun getOverflowHoeLevel(uuid: String?): Int? {
        uuid ?: return null
        val storage = gardenStorage?.overflowHoeLevels ?: return null
        if (storage.contains(uuid)) {
            return storage[uuid]
        } else {
            storage[uuid] = 0
            return 0
        }
    }

    private fun addOverflowHoeLevel(uuid: String?): Int? {
        uuid ?: return null
        val storage = gardenStorage?.overflowHoeLevels ?: return null
        val currentLevel = getOverflowHoeLevel(uuid) ?: return null
        storage[uuid] = currentLevel + 1
        return currentLevel + 1
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onRender(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        val renderable = display ?: return
        pos.renderRenderables(renderable, posLabel = "amazing")
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        hoeLevels = event.getConstant<GardenJson>("Garden").hoeExpLevels
        hoeOverflow = event.getConstant<GardenJson>("Garden").hoeExpOverflow
    }

    fun isEnabled() = config.enabled
}
