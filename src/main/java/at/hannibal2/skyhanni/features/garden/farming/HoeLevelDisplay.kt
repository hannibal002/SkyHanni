package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.GardenJson
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.UserLuckCalculateEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHoeExp
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHoeLevel
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getItemUuid
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.withColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

@SkyHanniModule
object HoeLevelDisplay {

    private const val OVERCLOCK_THRESHOLD = 40
    private const val MAX_LEVEL = 50

    private var hoeLevels: List<Int>? = null
    private var hoeOverflow = 200000
    private var display: List<Renderable>? = null
    private val gardenStorage get() = GardenApi.storage
    private val config get() = SkyHanniMod.feature.garden.hoeLevelDisplay

    private val patternGroup = RepoPattern.group("hoe.levels")

    /**
     * REGEX-TEST: OVERFLOW! Your Turing Sugar Cane Hoe Mk. III has just dropped a Tool Exp Capsule!
     */
    val levelUpPattern by patternGroup.pattern(
        "levelup-nocolor",
        "OVERFLOW! Your (?<tool>.+) has just dropped a Tool Exp Capsule!",
    )

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onTick() {
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
            if (hoeLevel >= OVERCLOCK_THRESHOLD) list.add(Renderable.text("§3§lOVERCLOCK REQUIRED!"))
            else list.add(Renderable.text("§c§lUPGRADE REQUIRED!"))
        }
        val formattedXp = hoeExp.addSeparators()
        val formattedXpToNext = next.addSeparators()
        list.add(Renderable.text("$colorPrefix$formattedXp§8/§e$formattedXpToNext"))

        display = list
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onChat(event: SkyHanniChatEvent.Modify) {
        val heldItem = InventoryUtils.getItemInHand() ?: return
        val leveledUpTool = levelUpPattern.matchMatcher(event.cleanMessage) {
            group("tool")
        } ?: return
        val heldItemName = heldItem.hoverName.string.removeColor()
        if (!heldItemName.contains(leveledUpTool)) return
        val overflowLevel = addOverflowHoeLevel(heldItem.getItemUuid())
        if (isEnabled() && config.overflow && overflowLevel != null) {
            val currentLevel = heldItem.getHoeLevel() ?: return
            val newComponent = event.chatComponent.copy().append(" §8(§3Level ${currentLevel + overflowLevel}§8)")
            event.replaceComponent(newComponent, "hoe_level")
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

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class, onlyOnIsland = IslandType.GARDEN)
    fun onRender() {
        if (!isEnabled()) return
        val renderable = display ?: return
        config.position.renderRenderables(renderable, posLabel = "Hoe Level Display")
    }

    @HandleEvent
    fun onUserLuck(event: UserLuckCalculateEvent) {
        if (!config.overflow) return
        val luck = calculateLuck()
        if (luck < 1) return
        event.addLuck(luck)
        val stack = ItemUtils.createItemStack(
            Items.NETHERITE_HOE,
            Component.literal("✴ Overflow Hoe Levels").withColor(ChatFormatting.GREEN),
            listOf(
                Component.literal("Items").withColor(ChatFormatting.DARK_GRAY),
                Component.empty(),
                componentBuilder {
                    appendWithColor("Value: ", ChatFormatting.GRAY)
                    appendWithColor("$luck✴", ChatFormatting.GREEN)
                },
                Component.empty(),
                Component.literal("Gain more by leveling up your farming tools!").withColor(ChatFormatting.DARK_GRAY),
            ),
        )
        event.addItem(stack)
    }

    private fun calculateLuck(): Float {
        val map = gardenStorage?.overflowHoeLevels ?: return 0f
        var luck = 0f
        for (entry in map) {
            luck += entry.value / 10
        }
        return luck
    }

    private fun errorNoTool() {
        ChatUtils.userError("You must hold a specialized farming tool to use this command!")
    }

    private fun errorStorage(item: ItemStack): Nothing {
        ErrorManager.skyHanniError(
            "Error getting overflow hoe level storage",
            "item" to item,
        )
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shsethoelevel") {
            category = CommandCategory.USERS_BUG_FIX
            description = "Manually sets your overflow hoe level"
            arg("level", BrigadierArguments.integer()) { newLevelArg ->
                callback {
                    val item = InventoryUtils.getItemInHand() ?: return@callback errorNoTool()
                    val uuid = item.getItemUuid() ?: return@callback errorNoTool()

                    val realLevel = item.getHoeLevel() ?: return@callback errorNoTool()
                    if (realLevel < MAX_LEVEL) {
                        ChatUtils.userError("Tools below level $MAX_LEVEL cannot have overflow levels!")
                        return@callback
                    }

                    val oldLevel = MAX_LEVEL + (getOverflowHoeLevel(uuid) ?: errorStorage(item))

                    val newLevel = getArg(newLevelArg)
                    if (newLevel < MAX_LEVEL) {
                        ChatUtils.userError("Overflow level cannot be below $MAX_LEVEL!")
                        return@callback
                    }
                    val storage = gardenStorage?.overflowHoeLevels ?: errorStorage(item)
                    storage[uuid] = newLevel - MAX_LEVEL
                    ChatUtils.chat(
                        componentBuilder {
                            append("Updated overflow level for ")
                            append(item.hoverName)
                            append(" from ")
                            appendWithColor(oldLevel.toString(), ChatFormatting.AQUA)
                            append(" to ")
                            appendWithColor(newLevel.toString(), ChatFormatting.AQUA)
                            append(".")
                        },
                    )
                }
            }
        }
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<GardenJson>("Garden")
        hoeLevels = data.hoeExpLevels
        hoeOverflow = data.hoeExpOverflow
    }

    fun isEnabled() = config.enabled
}
